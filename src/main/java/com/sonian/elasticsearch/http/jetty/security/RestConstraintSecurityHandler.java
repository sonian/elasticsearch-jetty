package com.sonian.elasticsearch.http.jetty.security;

import org.eclipse.jetty.http.security.Constraint;
import org.eclipse.jetty.security.*;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.util.StringMap;
import org.eclipse.jetty.util.TypeUtil;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;

/*
 * Handler to enforce SecurityConstraints. This implementation is
 * mostly copied from  org.eclipse.jetty.security.ConstraintSecurityHandler
 * but it is using ElasticSearch path specification instead of servlet spec.
 * It precomputes the constraint combinations for runtime efficiency.
 */
public class RestConstraintSecurityHandler extends SecurityHandler implements ConstraintAware {
    private final static String PATH_SPEC_SEPARATORS = ":,";

    private final List<ConstraintMapping> constraintMappings = new CopyOnWriteArrayList<ConstraintMapping>();
    private final Set<String> roles = new CopyOnWriteArraySet<String>();
    private final RestPathMap<Map<String, RoleInfo>> constraintMap = new RestPathMap<Map<String, RoleInfo>>();
    private boolean strict = true;

    /**
     * Get the strict mode.
     *
     * @return true if the security handler is running in strict mode.
     */
    public boolean isStrict() {
        return strict;
    }

    /**
     * Set the strict mode of the security handler.
     * <p/>
     * When in strict mode (the default), the full servlet specification
     * will be implemented.
     * If not in strict mode, some additional flexibility in configuration
     * is allowed:<ul>
     * <li>All users do not need to have a role defined in the deployment descriptor
     * <li>The * role in a constraint applies to ANY role rather than all roles defined in
     * the deployment descriptor.
     * </ul>
     *
     * @param strict the strict to set
     * @see #setRoles(Set)
     * @see #setConstraintMappings(List, Set)
     */
    public void setStrict(boolean strict) {
        this.strict = strict;
    }

    /**
     * @return Returns the constraintMappings.
     */
    public List<ConstraintMapping> getConstraintMappings() {
        return constraintMappings;
    }

    public Set<String> getRoles() {
        return roles;
    }

    /**
     * Process the constraints following the combining rules in Servlet 3.0 EA
     * spec section 13.7.1 Note that much of the logic is in the RoleInfo class.
     *
     * @param constraintMappings The constraintMappings to set, from which the set of known roles
     *                           is determined.
     */
    public void setConstraintMappings(List<ConstraintMapping> constraintMappings) {
        setConstraintMappings(constraintMappings, null);
    }

    /**
     * Process the constraints following the combining rules in Servlet 3.0 EA
     * spec section 13.7.1 Note that much of the logic is in the RoleInfo class.
     *
     * @param constraintMappings The constraintMappings to set as array, from which the set of known roles
     *                           is determined.  Needed to retain API compatibility for 7.x
     */
    public void setConstraintMappings(ConstraintMapping[] constraintMappings) {
        setConstraintMappings(Arrays.asList(constraintMappings), null);
    }

    /**
     * Process the constraints following the combining rules in Servlet 3.0 EA
     * spec section 13.7.1 Note that much of the logic is in the RoleInfo class.
     *
     * @param constraintMappings The constraintMappings to set.
     * @param roles              The known roles (or null to determine them from the mappings)
     */
    public void setConstraintMappings(List<ConstraintMapping> constraintMappings, Set<String> roles) {
        if (isStarted())
            throw new IllegalStateException("Started");
        this.constraintMappings.clear();
        this.constraintMappings.addAll(constraintMappings);

        if (roles == null) {
            roles = new HashSet<String>();
            for (ConstraintMapping cm : constraintMappings) {
                String[] cmr = cm.getConstraint().getRoles();
                if (cmr != null) {
                    for (String r : cmr)
                        if (!"*".equals(r))
                            roles.add(r);
                }
            }
        }
        setRoles(roles);
    }

    /**
     * Set the known roles.
     * This may be overridden by a subsequent call to {@link #setConstraintMappings(ConstraintMapping[])} or
     * {@link #setConstraintMappings(List, Set)}.
     *
     * @param roles The known roles (or null to determine them from the mappings)
     * @see #setStrict(boolean)
     */
    public void setRoles(Set<String> roles) {
        if (isStarted())
            throw new IllegalStateException("Started");

        this.roles.clear();
        this.roles.addAll(roles);
    }


    /**
     * @see org.eclipse.jetty.security.ConstraintAware#addConstraintMapping(org.eclipse.jetty.security.ConstraintMapping)
     */
    public void addConstraintMapping(ConstraintMapping mapping) {
        constraintMappings.add(mapping);
        if (mapping.getConstraint() != null && mapping.getConstraint().getRoles() != null)
            for (String role : mapping.getConstraint().getRoles())
                addRole(role);

        if (isStarted()) {
            processConstraintMapping(mapping);
        }
    }

    /**
     * @see org.eclipse.jetty.security.ConstraintAware#addRole(java.lang.String)
     */
    public void addRole(String role) {
        boolean modified = roles.add(role);
        if (isStarted() && modified && strict) {
            // Add the new role to currently defined any role role infos
            for (Map<String, RoleInfo> map : constraintMap.values()) {
                for (RoleInfo info : map.values()) {
                    if (info.isAnyRole())
                        info.addRole(role);
                }
            }
        }
    }

    /**
     * @see org.eclipse.jetty.security.SecurityHandler#doStart()
     */
    @Override
    protected void doStart() throws Exception {
        constraintMap.clear();
        if (constraintMappings != null) {
            for (ConstraintMapping mapping : constraintMappings) {
                processConstraintMapping(mapping);
            }
        }
        super.doStart();
    }

    @Override
    protected void doStop() throws Exception {
        constraintMap.clear();
        constraintMappings.clear();
        roles.clear();
        super.doStop();
    }

    @SuppressWarnings({"unchecked"})
    protected void processConstraintMapping(ConstraintMapping mapping) {
        String pathSpec = mapping.getPathSpec();

        StringTokenizer tok = new StringTokenizer(pathSpec, PATH_SPEC_SEPARATORS);

        while (tok.hasMoreTokens()) {
            String spec = tok.nextToken().trim();

            Map<String, RoleInfo> mappings = constraintMap.get(spec);
            if (mappings == null) {
                mappings = (Map<String, RoleInfo>) new StringMap();
                constraintMap.put(spec, mappings);
            }
            RoleInfo allMethodsRoleInfo = mappings.get(null);
            if (allMethodsRoleInfo != null && allMethodsRoleInfo.isForbidden())
                return;

            String httpMethod = mapping.getMethod();
            RoleInfo roleInfo = mappings.get(httpMethod);
            if (roleInfo == null) {
                roleInfo = new RoleInfo();
                mappings.put(httpMethod, roleInfo);
                if (allMethodsRoleInfo != null) {
                    roleInfo.combine(allMethodsRoleInfo);
                }
            }
            if (roleInfo.isForbidden())
                return;

            Constraint constraint = mapping.getConstraint();
            boolean forbidden = constraint.isForbidden();
            roleInfo.setForbidden(forbidden);
            if (forbidden) {
                if (httpMethod == null) {
                    mappings.clear();
                    mappings.put(null, roleInfo);
                }
            } else {
                UserDataConstraint userDataConstraint = UserDataConstraint.get(constraint.getDataConstraint());
                roleInfo.setUserDataConstraint(userDataConstraint);

                boolean checked = constraint.getAuthenticate();
                roleInfo.setChecked(checked);
                if (roleInfo.isChecked()) {
                    if (constraint.isAnyRole()) {
                        if (strict) {
                            // * means "all defined roles"
                            for (String role : roles)
                                roleInfo.addRole(role);
                        } else
                            // * means any role
                            roleInfo.setAnyRole(true);
                    } else {
                        String[] newRoles = constraint.getRoles();
                        for (String role : newRoles) {
                            if (strict && !roles.contains(role))
                                throw new IllegalArgumentException("Attempt to use undeclared role: " + role + ", known roles: " + roles);
                            roleInfo.addRole(role);
                        }
                    }
                }
                if (httpMethod == null) {
                    for (Map.Entry<String, RoleInfo> entry : mappings.entrySet()) {
                        if (entry.getKey() != null) {
                            RoleInfo specific = entry.getValue();
                            specific.combine(roleInfo);
                        }
                    }
                }
            }
        }
    }

    protected Object prepareConstraintInfo(String pathInContext, Request request) {
        Map<String, RoleInfo> mappings = constraintMap.match(pathInContext);

        if (mappings != null) {
            String httpMethod = request.getMethod();
            RoleInfo roleInfo = mappings.get(httpMethod);
            if (roleInfo == null)
                roleInfo = mappings.get(null);
            return roleInfo;
        }

        return null;
    }

    protected boolean checkUserDataPermissions(String pathInContext, Request request, Response response, Object constraintInfo) throws IOException {
        if (constraintInfo == null)
            return true;

        RoleInfo roleInfo = (RoleInfo) constraintInfo;
        if (roleInfo.isForbidden())
            return false;


        UserDataConstraint dataConstraint = roleInfo.getUserDataConstraint();
        if (dataConstraint == null || dataConstraint == UserDataConstraint.None) {
            return true;
        }
        HttpConnection connection = HttpConnection.getCurrentConnection();
        Connector connector = connection.getConnector();

        if (dataConstraint == UserDataConstraint.Integral) {
            if (connector.isIntegral(request))
                return true;
            if (connector.getConfidentialPort() > 0) {
                String url = connector.getIntegralScheme() + "://" + request.getServerName() + ":" + connector.getIntegralPort() + request.getRequestURI();
                if (request.getQueryString() != null)
                    url += "?" + request.getQueryString();
                response.setContentLength(0);
                response.sendRedirect(url);
            } else
                response.sendError(Response.SC_FORBIDDEN, "!Integral");

            request.setHandled(true);
            return false;
        } else if (dataConstraint == UserDataConstraint.Confidential) {
            if (connector.isConfidential(request))
                return true;

            if (connector.getConfidentialPort() > 0) {
                String url = connector.getConfidentialScheme() + "://" + request.getServerName() + ":" + connector.getConfidentialPort()
                        + request.getRequestURI();
                if (request.getQueryString() != null)
                    url += "?" + request.getQueryString();

                response.setContentLength(0);
                response.sendRedirect(url);
            } else
                response.sendError(Response.SC_FORBIDDEN, "!Confidential");

            request.setHandled(true);
            return false;
        } else {
            throw new IllegalArgumentException("Invalid dataConstraint value: " + dataConstraint);
        }

    }

    protected boolean isAuthMandatory(Request baseRequest, Response base_response, Object constraintInfo) {
        if (constraintInfo == null) {
            return false;
        }
        return ((RoleInfo) constraintInfo).isChecked();
    }

    @Override
    protected boolean checkWebResourcePermissions(String pathInContext, Request request, Response response, Object constraintInfo, UserIdentity userIdentity)
            throws IOException {
        if (constraintInfo == null) {
            return true;
        }
        RoleInfo roleInfo = (RoleInfo) constraintInfo;

        if (!roleInfo.isChecked()) {
            return true;
        }

        if (roleInfo.isAnyRole() && request.getAuthType() != null)
            return true;

        for (String role : roleInfo.getRoles()) {
            if (userIdentity.isUserInRole(role, null))
                return true;
        }
        return false;
    }

    /* ------------------------------------------------------------ */
    @Override
    public void dump(Appendable out, String indent) throws IOException {
        dumpThis(out);
        dump(out, indent, TypeUtil.asList(getHandlers()), getBeans(), Collections.singleton(roles), constraintMap.entrySet());
    }
}