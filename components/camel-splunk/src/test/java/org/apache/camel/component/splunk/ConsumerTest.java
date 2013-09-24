/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.component.splunk;

import java.io.InputStream;
import java.util.Map;

import com.splunk.Job;
import com.splunk.JobArgs;
import com.splunk.JobCollection;
import com.splunk.JobResultsArgs;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.component.splunk.event.SplunkEvent;
import org.junit.Test;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ConsumerTest extends SplunkMockTestSupport {
    @Test
    public void testSearch() throws Exception {
        MockEndpoint searchMock = getMockEndpoint("mock:search-result");
        searchMock.expectedMessageCount(3);
        searchMock.expectedPropertyReceived(Exchange.BATCH_SIZE, 3);
        JobCollection jobCollection = mock(JobCollection.class);
        Job jobMock = mock(Job.class);
        when(service.getJobs()).thenReturn(jobCollection);
        when(jobCollection.create(anyString(), any(JobArgs.class))).thenReturn(jobMock);
        when(jobMock.isDone()).thenReturn(Boolean.TRUE);
        InputStream stream = ConsumerTest.class.getResourceAsStream("/resultsreader_test_data.json");
        when(jobMock.getResults(any(JobResultsArgs.class))).thenReturn(stream);

        assertMockEndpointsSatisfied();
        SplunkEvent recieved = searchMock.getReceivedExchanges().get(0).getIn().getBody(SplunkEvent.class);
        assertNotNull(recieved);
        Map<String, String> data = recieved.getEventData();
        assertEquals("indexertpool", data.get("name"));
        assertEquals(true, searchMock.getReceivedExchanges().get(2).getProperty(Exchange.BATCH_COMPLETE, Boolean.class));
        stream.close();
    }

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            public void configure() {
                from("splunk://normal?delay=5s&username=foo&password=bar&initEarliestTime=-10s&latestTime=now&search=search index=myindex&sourceType=testSource")
                        .to("mock:search-result");
            }
        };
    }
}
