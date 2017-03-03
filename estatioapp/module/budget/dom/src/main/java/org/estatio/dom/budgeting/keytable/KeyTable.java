/*
 *
 *  Copyright 2012-2015 Eurocommercial Properties NV
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
package org.estatio.dom.budgeting.keytable;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.inject.Inject;
import javax.jdo.annotations.Column;
import javax.jdo.annotations.DatastoreIdentity;
import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.IdentityType;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.Queries;
import javax.jdo.annotations.Query;
import javax.jdo.annotations.Unique;
import javax.jdo.annotations.Version;
import javax.jdo.annotations.VersionStrategy;

import org.apache.isis.applib.annotation.Action;
import org.apache.isis.applib.annotation.ActionLayout;
import org.apache.isis.applib.annotation.CollectionLayout;
import org.apache.isis.applib.annotation.Contributed;
import org.apache.isis.applib.annotation.DomainObject;
import org.apache.isis.applib.annotation.MemberOrder;
import org.apache.isis.applib.annotation.Programmatic;
import org.apache.isis.applib.annotation.PropertyLayout;
import org.apache.isis.applib.annotation.RenderType;
import org.apache.isis.applib.annotation.RestrictTo;
import org.apache.isis.applib.annotation.SemanticsOf;
import org.apache.isis.applib.annotation.Where;

import org.isisaddons.module.security.dom.tenancy.ApplicationTenancy;

import org.incode.module.base.dom.utils.TitleBuilder;

import org.estatio.dom.UdoDomainObject2;
import org.estatio.dom.apptenancy.WithApplicationTenancyProperty;
import org.estatio.dom.asset.Unit;
import org.estatio.dom.asset.UnitRepository;
import org.estatio.dom.budgeting.Distributable;
import org.estatio.dom.budgeting.DistributionService;
import org.estatio.dom.budgeting.budget.Budget;
import org.estatio.dom.budgeting.keyitem.KeyItem;
import org.estatio.dom.budgeting.keyitem.KeyItemRepository;
import org.estatio.dom.budgeting.partioning.Partitioning;

import lombok.Getter;
import lombok.Setter;

@PersistenceCapable(
        identityType = IdentityType.DATASTORE
        ,schema = "dbo" // Isis' ObjectSpecId inferred from @DomainObject#objectType
)
@DatastoreIdentity(
        strategy = IdGeneratorStrategy.NATIVE,
        column = "id")
@Version(
        strategy = VersionStrategy.VERSION_NUMBER,
        column = "version")
@Queries({
        @Query(
                name = "findByPartitioningAndName", language = "JDOQL",
                value = "SELECT " +
                        "FROM org.estatio.dom.budgeting.keytable.KeyTable " +
                        "WHERE name == :name "
                        + "&& partitioning == :partitioning"),
        @Query(
                name = "findByPartioning", language = "JDOQL",
                value = "SELECT " +
                        "FROM org.estatio.dom.budgeting.keytable.KeyTable " +
                        "WHERE partitioning == :partitioning "),
        @Query(
                name = "findKeyTableByNameMatches", language = "JDOQL",
                value = "SELECT " +
                        "FROM org.estatio.dom.budgeting.keytable.KeyTable " +
                        "WHERE name.toLowerCase().indexOf(:name) >= 0 ")
})
@Unique(name = "KeyTable_partitioning_name", members = { "partitioning", "name" })
@DomainObject(
        autoCompleteRepository = KeyTableRepository.class,
        autoCompleteAction = "autoComplete",
        objectType = "org.estatio.dom.budgeting.keytable.KeyTable"
)
public class KeyTable extends UdoDomainObject2<Budget> implements WithApplicationTenancyProperty {

    public KeyTable() {
        super("name, partitioning");
    }

    public String title() {
        return TitleBuilder
                .start()
                .withParent(getPartitioning())
                .withName(getName())
                .toString();
    }

    public String toString() {
        return this.getName();
    }

    @Column(name = "partitionId", allowsNull = "false")
    @PropertyLayout(hidden = Where.PARENTED_TABLES)
    @Getter @Setter
    private Partitioning partitioning;

    @Column(allowsNull = "false")
    @Getter @Setter
    private String name;

    @Action(semantics = SemanticsOf.IDEMPOTENT)
    public KeyTable changeName(final String name) {
        setName(name);
        return this;
    }

    public String validateChangeName(final String name) {
        if (name.equals(null)) {
            return "Name can't be empty";
        }
        if (keyTableRepository.findByPartitioningAndName(getPartitioning(), name) != null) {
            return "There is already a keytable with this name for this partitioning";
        }
        return null;
    }

    public String default0ChangeName(final String name) {
        return getName();
    }

    @Column(name = "foundationValueTypeId", allowsNull = "false")
    @Getter @Setter
    private FoundationValueType foundationValueType;

    @Action(semantics = SemanticsOf.IDEMPOTENT)
    public KeyTable changeFoundationValueType(
            final FoundationValueType foundationValueType) {
        setFoundationValueType(foundationValueType);
        return this;
    }

    public FoundationValueType default0ChangeFoundationValueType(final FoundationValueType foundationValueType) {
        return getFoundationValueType();
    }

    public String validateChangeFoundationValueType(final FoundationValueType foundationValueType) {
        if (foundationValueType.equals(null)) {
            return "Foundation value type can't be empty";
        }
        return null;
    }

    @Column(name = "keyValueMethodId", allowsNull = "false")
    @Getter @Setter
    private KeyValueMethod keyValueMethod;

    @Action(semantics = SemanticsOf.IDEMPOTENT)
    public KeyTable changeKeyValueMethod(
            final KeyValueMethod keyValueMethod) {
        setKeyValueMethod(keyValueMethod);
        return this;
    }

    public KeyValueMethod default0ChangeKeyValueMethod(final KeyValueMethod keyValueMethod) {
        return getKeyValueMethod();
    }

    public String validateChangeKeyValueMethod(final KeyValueMethod keyValueMethod) {
        if (keyValueMethod.equals(null)) {
            return "Key value method can't be empty";
        }
        return null;
    }

    @PropertyLayout(hidden = Where.EVERYWHERE)
    @Column(allowsNull = "false")
    @Getter @Setter
    private Integer precision;

    @Action(semantics = SemanticsOf.IDEMPOTENT)
    @ActionLayout(hidden = Where.EVERYWHERE)
    public KeyTable changePrecision(
            final Integer numberOfDigits) {
        setPrecision(numberOfDigits);
        return this;
    }

    public Integer default0ChangePrecision(final Integer numberOfDigits) {
        return getPrecision();
    }

    public String validateChangePrecision(final Integer numberOfDigits) {
        if (numberOfDigits < 0 || numberOfDigits > 6) {
            return "Number Of Digits must have a value between 0 and 6";
        }
        return null;
    }

    @CollectionLayout(render = RenderType.EAGERLY)
    @Persistent(mappedBy = "keyTable", dependentElement = "true")
    @Getter @Setter
    private SortedSet<KeyItem> items = new TreeSet<>();

    @Action(semantics = SemanticsOf.NON_IDEMPOTENT)
    @ActionLayout(contributed = Contributed.AS_ACTION)
    public KeyItem newItem(
            final Unit unit,
            final BigDecimal sourceValue,
            final BigDecimal keyValue) {

        return keyItemRepository.newItem(this, unit, sourceValue, keyValue);
    }

    public String validateNewItem(
            final Unit unit,
            final BigDecimal sourceValue,
            final BigDecimal keyValue) {

        return keyItemRepository.validateNewItem(this, unit, sourceValue, keyValue);
    }

    @Action(semantics = SemanticsOf.NON_IDEMPOTENT_ARE_YOU_SURE)
    public KeyTable generateItems() {

        //delete old items
        for (Iterator<KeyItem> it = this.getItems().iterator(); it.hasNext(); ) {
            it.next().deleteBudgetKeyItem();
        }

        /*
        create list of input pairs: identifier - sourcevalue
        sourcevalue is determined by FoundationValueType
        */
        List<Distributable> input = new ArrayList<>();

        for (Unit unit : unitRepository.findByProperty(this.getPartitioning().getProperty())) {

            if (unitIntervalValidForThisKeyTable(unit)) {
                BigDecimal sourceValue;
                if (getFoundationValueType().valueOf(unit) != null) {
                    sourceValue = getFoundationValueType().valueOf(unit);
                } else {
                    sourceValue = BigDecimal.ZERO;
                }
                KeyItem newItem = new KeyItem();
                newItem.setSourceValue(sourceValue);
                newItem.setValue(BigDecimal.ZERO);
                newItem.setUnit(unit);
                newItem.setKeyTable(this);
                persistIfNotAlready(newItem);
                input.add(newItem);
            }
        }

        /*
        call distribute method
         */
        DistributionService distributionService = new DistributionService();
        distributionService.distribute(input, getKeyValueMethod().divider(this), getPrecision());

        return this;
    }

    public boolean hideGenerateItems() {
        if (getFoundationValueType() == FoundationValueType.MANUAL) {
            return true;
        }
        return false;
    }

    // //////////////////////////////////////

    @MemberOrder(name = "items", sequence = "4")
    @Action(semantics = SemanticsOf.NON_IDEMPOTENT_ARE_YOU_SURE)
    public KeyTable distributeSourceValues() {

        DistributionService distributionService = new DistributionService();
        distributionService.distribute(new ArrayList(getItems()), getKeyValueMethod().divider(this), getPrecision());

        return this;
    }

    // //////////////////////////////////////

    @Override
    @PropertyLayout(hidden = Where.EVERYWHERE)
    public ApplicationTenancy getApplicationTenancy() {
        return getPartitioning().getProperty().getApplicationTenancy();
    }

    // //////////////////////////////////////
    @PropertyLayout(hidden = Where.EVERYWHERE)
    public boolean isValid() {
        return (this.isValidForKeyValues() && this.isValidForUnits());
    }

    public boolean isValidForKeyValues() {
        return getKeyValueMethod().isValid(this);
    }

    @PropertyLayout(hidden = Where.EVERYWHERE)
    public boolean isValidForUnits() {
        for (KeyItem item : this.getItems()) {
            if (!this.unitIntervalValidForThisKeyTable(item.getUnit())) {
                return false;
            }

            // previously we had this code, but no longer sure if it makes any sense...
//            final Unit unit = item.getUnit();
//            final LocalDateInterval interval = getBudget().getInterval();
//            if (occupancyRepository.occupanciesByUnitAndInterval(unit, interval).isEmpty()) {
//                return false;
//            }

        }
        return true;
    }

    @Programmatic
    private boolean unitIntervalValidForThisKeyTable(final Unit unit) {
        return unit.getInterval().contains(getPartitioning().getInterval());
    }

    @Programmatic
    public void createCopyOn(final Budget newBudget) {
        KeyTable copiedTable = newBudget.createKeyTable(getName(), getFoundationValueType(), getKeyValueMethod());
        for (KeyItem item : getItems()){
            copiedTable.newItem(item.getUnit(), item.getSourceValue(), item.getValue());
        }
    }

    // //////////////////////////////////////

    @Action(restrictTo = RestrictTo.PROTOTYPING, semantics = SemanticsOf.NON_IDEMPOTENT_ARE_YOU_SURE)
    public KeyTable deleteItems() {
        for (KeyItem keyItem : getItems()) {
            removeIfNotAlready(keyItem);
        }

        return this;
    }

    // //////////////////////////////////////

    @Inject
    UnitRepository unitRepository;

    @Inject
    KeyTableRepository keyTableRepository;

    @Inject
    KeyItemRepository keyItemRepository;


}
