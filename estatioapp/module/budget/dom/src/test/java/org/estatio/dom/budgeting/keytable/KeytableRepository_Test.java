/*
 * Copyright 2015 Yodo Int. Projects and Consultancy
 *
 * Licensed under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.estatio.dom.budgeting.keytable;

import java.util.Arrays;
import java.util.List;

import org.jmock.Expectations;
import org.jmock.auto.Mock;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.apache.isis.applib.DomainObjectContainer;
import org.apache.isis.applib.query.Query;
import org.apache.isis.core.unittestsupport.jmocking.JUnitRuleMockery2;

import org.incode.module.unittestsupport.dom.repo.FinderInteraction;

import org.estatio.dom.budgeting.partioning.Partitioning;

import static org.assertj.core.api.Assertions.assertThat;

public class KeytableRepository_Test {

    FinderInteraction finderInteraction;

    KeyTableRepository keyTableRepository;

    @Before
    public void setup() {
        keyTableRepository = new KeyTableRepository() {

            @Override
            protected <T> T firstMatch(Query<T> query) {
                finderInteraction = new FinderInteraction(query, FinderInteraction.FinderMethod.FIRST_MATCH);
                return null;
            }

            @Override
            protected <T> T uniqueMatch(Query<T> query) {
                finderInteraction = new FinderInteraction(query, FinderInteraction.FinderMethod.UNIQUE_MATCH);
                return null;
            }

            @Override
            protected List<KeyTable> allInstances() {
                finderInteraction = new FinderInteraction(null, FinderInteraction.FinderMethod.ALL_INSTANCES);
                return null;
            }

            @Override
            protected <T> List<T> allMatches(Query<T> query) {
                finderInteraction = new FinderInteraction(query, FinderInteraction.FinderMethod.ALL_MATCHES);
                return null;
            }
        };
    }

    public static class FindByPropertyAndNameAndStartDate extends KeytableRepository_Test {

        @Test
        public void happyCase() {

            Partitioning partitioning = new Partitioning();
            String name = "KeyTableName";
            keyTableRepository.findByPartitioningAndName(partitioning, name);

            assertThat(finderInteraction.getFinderMethod()).isEqualTo(FinderInteraction.FinderMethod.UNIQUE_MATCH);
            assertThat(finderInteraction.getResultType()).isEqualTo(KeyTable.class);
            assertThat(finderInteraction.getQueryName()).isEqualTo("findByPartitioningAndName");
            assertThat(finderInteraction.getArgumentsByParameterName().get("partitioning")).isEqualTo((Object) partitioning);
            assertThat(finderInteraction.getArgumentsByParameterName().get("name")).isEqualTo((Object) name);
            assertThat(finderInteraction.getArgumentsByParameterName()).hasSize(2);
        }



    }

    public static class FindByPartitioning extends KeytableRepository_Test {

        @Test
        public void happyCase() {

            Partitioning partitioning = new Partitioning();
            keyTableRepository.findByPartioning(partitioning);

            assertThat(finderInteraction.getFinderMethod()).isEqualTo(FinderInteraction.FinderMethod.ALL_MATCHES);
            assertThat(finderInteraction.getResultType()).isEqualTo(KeyTable.class);
            assertThat(finderInteraction.getQueryName()).isEqualTo("findByPartioning");
            assertThat(finderInteraction.getArgumentsByParameterName().get("partitioning")).isEqualTo((Object) partitioning);
            assertThat(finderInteraction.getArgumentsByParameterName()).hasSize(1);
        }



    }


    public static class NewKeyTable extends KeytableRepository_Test {

        @Rule
        public JUnitRuleMockery2 context = JUnitRuleMockery2.createFor(JUnitRuleMockery2.Mode.INTERFACES_AND_CLASSES);

        @Mock
        private DomainObjectContainer mockContainer;

        KeyTableRepository keyTableRepositoryRepo;

        @Before
        public void setup() {
            keyTableRepositoryRepo = new KeyTableRepository() {
                @Override
                public List<KeyTable> findByPartioning(final Partitioning partitioning) {
                    return Arrays.asList(new KeyTable());
                }
            };
            keyTableRepositoryRepo.setContainer(mockContainer);
        }

        @Test
        public void newKeyTable() {

            //given
            Partitioning partitioning = new Partitioning();
            final KeyTable keyTable = new KeyTable();

            // expect
            context.checking(new Expectations() {
                {
                    oneOf(mockContainer).newTransientInstance(KeyTable.class);
                    will(returnValue(keyTable));
                    oneOf(mockContainer).persistIfNotAlready(keyTable);
                }

            });

            //when
            KeyTable newKeyTable = keyTableRepositoryRepo.newKeyTable(
                    partitioning,
                    "new keyTable",
                    FoundationValueType.AREA,
                    KeyValueMethod.PERCENT,
                    6);

            //then
            assertThat(newKeyTable.getPartitioning()).isEqualTo(partitioning);
            assertThat(newKeyTable.getName()).isEqualTo("new keyTable");
            assertThat(newKeyTable.getFoundationValueType()).isEqualTo(FoundationValueType.AREA);
            assertThat(newKeyTable.getKeyValueMethod()).isEqualTo(KeyValueMethod.PERCENT);
            assertThat(newKeyTable.getPrecision()).isEqualTo(6);
        }

    }

}
