package org.elasticsearch.http.jetty.security;

import org.elasticsearch.common.path.PathTrie;
import org.elasticsearch.rest.support.RestUtils;

import java.util.*;

/**
 * @author imotov
 */
public class RestPathMap<T> extends HashMap<String, T> {

    private volatile PathTrie<Entry<String, T>> pathTrie = newPathTrie();
    private volatile Entry<String, T> defaultEntry = null;

    public RestPathMap() {
        super(11);
    }

    private PathTrie<Entry<String, T>> newPathTrie() {
        return new PathTrie<Entry<String, T>>(RestUtils.REST_DECODER);
    }

    /* --------------------------------------------------------------- */

    /**
     * Add a single path match to the PathMap.
     *
     * @param pathSpec The path specification, or comma separated list of
     *                 path specifications.
     * @param object   The object the path maps to
     */
    @Override
    public T put(String pathSpec, T object) {
        T old = null;
        if (!pathSpec.startsWith("/") && !pathSpec.equals("*"))
            throw new IllegalArgumentException("PathSpec " + pathSpec + ". must start with '/'");

        old = super.put(pathSpec, object);

        // Make entry that was just created.
        Entry<String, T> entry = new Entry<String, T>(pathSpec, object);

        if (pathSpec.equals("*")) {
            defaultEntry = entry;
        } else {
            pathTrie.insert(pathSpec, entry);
        }

        return old;
    }

    /* ------------------------------------------------------------ */

    /**
     * Get object matched by the path.
     *
     * @param path the path.
     * @return Best matched object or null.
     */
    public T match(String path) {
        Map.Entry<String, T> entry = getMatch(path);
        if (entry != null)
            return entry.getValue();
        return null;
    }


    /* --------------------------------------------------------------- */

    /**
     * Get the entry mapped by the best specification.
     *
     * @param path the path.
     * @return Map.Entry of the best matched  or null.
     */
    public Entry<String, T> getMatch(String path) {
        if (path == null)
            return null;

        Entry<String, T> match = pathTrie.retrieve(path);
        if (match != null) {
            return match;
        }

        return defaultEntry;
    }

    /* --------------------------------------------------------------- */

    /**
     * Return whether the path matches any entries in the PathMap,
     * excluding the default entry
     *
     * @param path Path to match
     * @return Whether the PathMap contains any entries that match this
     */
    public boolean containsMatch(String path) {
        Entry match = getMatch(path);
        return match != null;
    }

    /* --------------------------------------------------------------- */
    @Override
    public void clear() {
        pathTrie = newPathTrie();
        defaultEntry = null;
        super.clear();
    }

    public static class Entry<K, V> implements Map.Entry<K, V> {
        private final K key;
        private final V value;
        private transient String string;

        Entry(K key, V value) {
            this.key = key;
            this.value = value;
        }

        public K getKey() {
            return key;
        }

        public V getValue() {
            return value;
        }

        public V setValue(V o) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String toString() {
            if (string == null)
                string = key + "=" + value;
            return string;
        }

    }
}
