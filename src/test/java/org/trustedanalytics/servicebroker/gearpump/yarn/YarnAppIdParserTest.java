/**
 * Copyright (c) 2015 Intel Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.trustedanalytics.servicebroker.gearpump.yarn;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class YarnAppIdParserTest {

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidApplicationId() {
        String applicationId = "illegal_id";
        new YarnAppIdParser(applicationId);
    }

    @Test
    public void testValidApplicationId() {
        String applicationId = "application_1234567890123_1";
        new YarnAppIdParser(applicationId);
    }

}
