/**
 * Copyright (c) 2017 Intel Corporation
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

package org.trustedanalytics.servicebroker.gearpump.service.dashboard;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.util.Optional;

@RunWith(MockitoJUnitRunner.class)
public class DashboardStateValidatorTest {

    private DashboardStateValidator validator;

    @Before
    public void before() throws IOException {
        validator = new DashboardStateValidator();
        validator.disableRetrials();
    }

    @Test
    public void test_validate() throws Exception {
        Assert.assertEquals(false, validator.validate(() -> Optional.of(false)));
        Assert.assertEquals(false, validator.validate(Optional::empty));
        Assert.assertEquals(true, validator.validate(() -> Optional.of(true)));
    }
}
