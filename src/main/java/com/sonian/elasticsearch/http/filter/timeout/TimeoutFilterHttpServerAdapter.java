package com.sonian.elasticsearch.http.filter.timeout;

import com.sonian.elasticsearch.http.filter.FilterChain;
import com.sonian.elasticsearch.http.filter.FilterHttpServerAdapter;
import org.elasticsearch.ElasticSearchParseException;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.inject.assistedinject.Assisted;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.http.HttpChannel;
import org.elasticsearch.http.HttpRequest;

/**
 * @author imotov
 */
public class TimeoutFilterHttpServerAdapter implements FilterHttpServerAdapter {
    private static final String SLEEP_PARAM = "sleep";

    @Inject
    public TimeoutFilterHttpServerAdapter(@Assisted String name, @Assisted Settings settings) {

    }

    @Override
    public void doFilter(HttpRequest request, HttpChannel channel, FilterChain filterChain) {
        if (request.hasParam(SLEEP_PARAM)) {
            long sleep = -1;
            try {
                sleep = request.paramAsTime(SLEEP_PARAM, TimeValue.timeValueMillis(0)).millis();
            } catch (ElasticSearchParseException ex) {
                // Ignore
            }
            if (sleep > 0) {
                try {
                    Thread.sleep(sleep);
                } catch (InterruptedException e) {
                    // Ignore
                }
            }
        }
        filterChain.doFilter(request, channel);
    }
}
