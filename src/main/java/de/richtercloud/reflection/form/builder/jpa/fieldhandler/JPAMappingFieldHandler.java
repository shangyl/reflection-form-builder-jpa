/**
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.richtercloud.reflection.form.builder.jpa.fieldhandler;

import de.richtercloud.message.handler.IssueHandler;
import de.richtercloud.reflection.form.builder.ComponentHandler;
import de.richtercloud.reflection.form.builder.ResetException;
import de.richtercloud.reflection.form.builder.components.money.AmountMoneyCurrencyStorage;
import de.richtercloud.reflection.form.builder.components.money.AmountMoneyExchangeRateRetriever;
import de.richtercloud.reflection.form.builder.fieldhandler.AmountMoneyFieldHandler;
import de.richtercloud.reflection.form.builder.fieldhandler.FieldHandler;
import de.richtercloud.reflection.form.builder.fieldhandler.FieldHandlingException;
import de.richtercloud.reflection.form.builder.fieldhandler.FieldUpdateEvent;
import de.richtercloud.reflection.form.builder.fieldhandler.FieldUpdateListener;
import de.richtercloud.reflection.form.builder.fieldhandler.MappingFieldHandler;
import de.richtercloud.reflection.form.builder.fieldhandler.factory.AmountMoneyMappingFieldHandlerFactory;
import de.richtercloud.reflection.form.builder.jpa.JPAReflectionFormBuilder;
import de.richtercloud.reflection.form.builder.jpa.fieldhandler.factory.JPAAmountMoneyMappingFieldHandlerFactory;
import de.richtercloud.reflection.form.builder.jpa.idapplier.IdApplier;
import de.richtercloud.reflection.form.builder.jpa.panels.LongIdPanel;
import de.richtercloud.reflection.form.builder.jpa.panels.QueryHistoryEntryStorage;
import de.richtercloud.reflection.form.builder.jpa.storage.FieldInitializer;
import de.richtercloud.reflection.form.builder.jpa.storage.PersistenceStorage;
import de.richtercloud.reflection.form.builder.jpa.typehandler.ElementCollectionTypeHandler;
import de.richtercloud.reflection.form.builder.jpa.typehandler.ToManyTypeHandler;
import de.richtercloud.reflection.form.builder.jpa.typehandler.ToOneTypeHandler;
import de.richtercloud.reflection.form.builder.jpa.typehandler.factory.JPAAmountMoneyMappingTypeHandlerFactory;
import de.richtercloud.reflection.form.builder.panels.NumberPanel;
import de.richtercloud.reflection.form.builder.panels.NumberPanelUpdateEvent;
import de.richtercloud.validation.tools.FieldRetriever;
import java.awt.Component;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.swing.JComponent;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Handles entities and embeddables differently based on two type component-{@link FieldHandler} mappings.
 *
 * @author richter
 * @param <T> the type of entity to handle
 * @param <E> the type of field updates to expect
 */
public class JPAMappingFieldHandler<T, E extends FieldUpdateEvent<T>> extends MappingFieldHandler<T,E, JPAReflectionFormBuilder, Component> {
    private final static ComponentHandler<LongIdPanel> LONG_ID_PANEL_COMPONENT_RESETTER = (LongIdPanel component) -> {
        component.reset();
    };
    private final ElementCollectionTypeHandler elementCollectionTypeHandler;
    private final Map<Type, FieldHandler<?,?,?, ?>> embeddableMapping;
    private final ToManyTypeHandler toManyTypeHandler;
    private final ToOneTypeHandler toOneTypeHandler;
    private final IssueHandler issueHandler;
    private final IdApplier idApplier;

    public static JPAMappingFieldHandler create(PersistenceStorage storage,
            int initialQueryLimit,
            IssueHandler issueHandler,
            FieldRetriever fieldRetriever,
            AmountMoneyCurrencyStorage amountMoneyCurrencyStorage,
            AmountMoneyExchangeRateRetriever amountMoneyExchangeRateRetriever,
            String bidirectionalHelpDialogTitle,
            IdApplier idApplier,
            FieldInitializer fieldInitializer,
            QueryHistoryEntryStorage entryStorage,
            FieldRetriever readOnlyFieldRetriever) {
        JPAAmountMoneyMappingFieldHandlerFactory jPAAmountMoneyClassMappingFactory = new JPAAmountMoneyMappingFieldHandlerFactory(storage,
                initialQueryLimit,
                issueHandler,
                amountMoneyCurrencyStorage,
                amountMoneyExchangeRateRetriever,
                readOnlyFieldRetriever);
        AmountMoneyMappingFieldHandlerFactory amountMoneyClassMappingFactory = new AmountMoneyMappingFieldHandlerFactory(amountMoneyCurrencyStorage,
                amountMoneyExchangeRateRetriever,
                issueHandler);
        JPAAmountMoneyMappingTypeHandlerFactory jPAAmountMoneyTypeHandlerMappingFactory = new JPAAmountMoneyMappingTypeHandlerFactory(storage,
                initialQueryLimit,
                issueHandler,
                readOnlyFieldRetriever);
        AmountMoneyFieldHandler amountMoneyFieldHandler = new AmountMoneyFieldHandler(amountMoneyExchangeRateRetriever,
                amountMoneyCurrencyStorage,
                issueHandler);
        ElementCollectionTypeHandler elementCollectionTypeHandler = new ElementCollectionTypeHandler(jPAAmountMoneyTypeHandlerMappingFactory.generateTypeHandlerMapping(),
                jPAAmountMoneyTypeHandlerMappingFactory.generateTypeHandlerMapping(),
                issueHandler,
                amountMoneyFieldHandler,
                readOnlyFieldRetriever);
        ToManyTypeHandler toManyTypeHandler = new ToManyTypeHandler(storage,
                issueHandler,
                jPAAmountMoneyTypeHandlerMappingFactory.generateTypeHandlerMapping(),
                jPAAmountMoneyTypeHandlerMappingFactory.generateTypeHandlerMapping(),
                bidirectionalHelpDialogTitle,
                fieldInitializer,
                entryStorage,
                readOnlyFieldRetriever);
        ToOneTypeHandler toOneTypeHandler = new ToOneTypeHandler(storage,
                issueHandler,
                bidirectionalHelpDialogTitle,
                fieldInitializer,
                entryStorage,
                readOnlyFieldRetriever);
        return new JPAMappingFieldHandler(jPAAmountMoneyClassMappingFactory.generateClassMapping(),
                amountMoneyClassMappingFactory.generateClassMapping(),
                jPAAmountMoneyClassMappingFactory.generatePrimitiveMapping(),
                elementCollectionTypeHandler,
                toManyTypeHandler,
                toOneTypeHandler,
                issueHandler,
                idApplier);
    }

    public JPAMappingFieldHandler(Map<Type, FieldHandler<?, ?,?, ?>> classMapping,
            Map<Type, FieldHandler<?,?,?, ?>> embeddableMapping,
            Map<Class<?>, FieldHandler<?, ?,?, ?>> primitiveMapping,
            ElementCollectionTypeHandler elementCollectionTypeHandler,
            ToManyTypeHandler oneToManyTypeHandler,
            ToOneTypeHandler toOneTypeHandler,
            IssueHandler issueHandler,
            IdApplier idApplier) {
        super(classMapping,
                primitiveMapping,
                issueHandler);
        this.elementCollectionTypeHandler = elementCollectionTypeHandler;
        this.embeddableMapping = embeddableMapping;
        this.toManyTypeHandler = oneToManyTypeHandler;
        this.toOneTypeHandler = toOneTypeHandler;
        this.issueHandler = issueHandler;
        this.idApplier = idApplier;
    }

    public IssueHandler getIssueHandler() {
        return issueHandler;
    }

    public IdApplier getIdApplier() {
        return idApplier;
    }

    @Override
    protected Pair<JComponent, ComponentHandler<?>> handle0(Field field,
            Object instance,
            final FieldUpdateListener updateListener,
            JPAReflectionFormBuilder reflectionFormBuilder) throws FieldHandlingException,
            ResetException {
        if(field == null) {
            throw new IllegalArgumentException("fieldClass mustn't be null");
        }
        Object fieldValue;
        try {
            fieldValue = field.get(instance);
        } catch (IllegalArgumentException | IllegalAccessException ex) {
            throw new FieldHandlingException(ex);
        }
        Type fieldType = field.getGenericType();
        if(field.getAnnotation(Id.class) != null) {
            if(!(fieldType instanceof Class)) {
                throw new IllegalArgumentException("@Id annotated field has to be a class");
            }
            Class<?> fieldTypeClass = (Class<?>) fieldType;
            if(fieldTypeClass.equals(Long.class)) {
                Long fieldValueCast;
                try {
                    fieldValueCast = (Long) field.get(instance);
                } catch (IllegalArgumentException
                        | IllegalAccessException ex) {
                    throw new FieldHandlingException(ex);
                }
                NumberPanel<Long> retValue;
                if(fieldType.equals(Long.class)) {
                    retValue = new LongIdPanel(instance,
                            fieldValueCast, //initialValue
                            issueHandler,
                            false, //readOnly
                            idApplier);
                }else {
                    throw new IllegalArgumentException(String.format("field type %s is not supported", fieldValue.getClass()));
                }
                retValue.addUpdateListener((NumberPanelUpdateEvent<Long> event) -> {
                    updateListener.onUpdate(new FieldUpdateEvent<>(event.getNewValue()));
                });
                return new ImmutablePair<>(retValue, LONG_ID_PANEL_COMPONENT_RESETTER);
            }else {
                throw new IllegalArgumentException(String.format("@Id annotated field type %s not supported", field.getGenericType()));
            }
        }
        String fieldName = field.getName();
        Class<?> fieldDeclaringClass = field.getDeclaringClass();
        if(field.getAnnotation(ElementCollection.class) != null) {
            //can't be handled differently because otherwise a QueryPanel would
            //be tried to be used and IllegalArgumentException thrown at
            //initialization
            if(fieldValue != null && !(fieldValue instanceof List)) {
                throw new IllegalArgumentException("field values isn't an instance of List");
            }
            Pair<JComponent, ComponentHandler<?>> retValue = this.elementCollectionTypeHandler.handle(field.getGenericType(),
                    (List<Object>)fieldValue,
                    fieldName,
                    fieldDeclaringClass,
                    updateListener,
                    reflectionFormBuilder);
            return retValue;
        }
        if(field.getAnnotation(OneToMany.class) != null || field.getAnnotation(ManyToMany.class) != null) {
            Pair<JComponent, ComponentHandler<?>> retValue = this.toManyTypeHandler.handle(field.getGenericType(),
                    (List<Object>)fieldValue,
                    fieldName,
                    fieldDeclaringClass,
                    updateListener,
                    reflectionFormBuilder);
            return retValue;
        }
        if(field.getAnnotation(OneToOne.class) != null || field.getAnnotation(ManyToOne.class) != null) {
            Pair<JComponent, ComponentHandler<?>> retValue = this.toOneTypeHandler.handle(field.getGenericType(),
                    fieldValue,
                    fieldName,
                    fieldDeclaringClass,
                    updateListener,
                    reflectionFormBuilder);
            return retValue;
        }
        if(field.getType() instanceof Class) {
            Class<?> fieldTypeClass = field.getType();
            if(fieldTypeClass.getAnnotation(Embeddable.class) != null) {
                FieldHandler fieldHandler = embeddableMapping.get(fieldType);
                JComponent retValue = fieldHandler.handle(field,
                        instance,
                        updateListener,
                        reflectionFormBuilder);
                return new ImmutablePair<>(retValue,
                        fieldHandler);
            }
        }
        return super.handle0(field,
                instance,
                updateListener,
                reflectionFormBuilder);
    }
}