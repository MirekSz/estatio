/*
 *
 *  Copyright 2012-2014 Eurocommercial Properties NV
 *
 *
 *  Licensed under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.estatio.fixture;

import java.util.List;
import javax.inject.Inject;
import org.estatio.dom.currency.Currencies;
import org.estatio.dom.currency.Currency;
import org.apache.isis.applib.fixturescripts.CompositeFixtureScript;

/**
 * Will reset to a fixed baseline of the {@link org.estatio.fixture.EstatioRefDataSetupFixture reference data}
 * but no transactional data.
 *
 * <p>
 * As a minor optimization, the script checks if any (immutable read-only) reference data exists, is only installs
 * it the first time (an idempotent operation).
 * </p>
 */
public class EstatioBaseLineFixture extends CompositeFixtureScript {

    public EstatioBaseLineFixture() {
        super(null, "baseline");
    }

    @Override
    protected void execute(ExecutionContext executionContext) {
        execute(new ClockFixture(), executionContext);
        execute(new EstatioOperationalTeardownFixture(), executionContext);
        if(isRefDataPresent()) {
            return;
        }
        execute(new EstatioRefDataSetupFixture(), executionContext);
    }

    /**
     * Use the presence of any persisted {@link Currency} as the indicator as to whether
     * any reference data has previously been {@link EstatioRefDataSetupFixture setup}.
     */
    private boolean isRefDataPresent() {
        final List<Currency> currencyList = currencies.allCurrencies();
        return !currencyList.isEmpty();
    }

    @Inject
    private Currencies currencies;

}