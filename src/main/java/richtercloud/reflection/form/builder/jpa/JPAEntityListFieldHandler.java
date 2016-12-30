/**
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package richtercloud.reflection.form.builder.jpa;

import java.util.List;
import javax.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import richtercloud.message.handler.MessageHandler;
import richtercloud.reflection.form.builder.fieldhandler.AbstractListFieldHandler;
import richtercloud.reflection.form.builder.fieldhandler.FieldHandler;
import richtercloud.reflection.form.builder.fieldhandler.FieldUpdateEvent;
import richtercloud.reflection.form.builder.jpa.panels.InitialQueryTextGenerator;
import richtercloud.reflection.form.builder.jpa.typehandler.JPAEntityListTypeHandler;
import richtercloud.reflection.form.builder.panels.AbstractListPanel;
import richtercloud.reflection.form.builder.jpa.storage.PersistenceStorage;
import richtercloud.reflection.form.builder.typehandler.TypeHandler;
import richtercloud.reflection.form.builder.jpa.storage.FieldInitializer;

/**
 *
 * @author richter
 */
public class JPAEntityListFieldHandler extends AbstractListFieldHandler<List<Object>, FieldUpdateEvent<List<Object>>, JPAReflectionFormBuilder> implements FieldHandler<List<Object>,FieldUpdateEvent<List<Object>>, JPAReflectionFormBuilder, AbstractListPanel> {
    private final static Logger LOGGER = LoggerFactory.getLogger(JPAEntityListFieldHandler.class);

    public JPAEntityListFieldHandler(PersistenceStorage storage,
            MessageHandler messageHandler,
            String bidirectionalHelpDialogTitle,
            FieldInitializer fieldInitializer,
            InitialQueryTextGenerator initialQueryTextGenerator) {
        super(messageHandler,
                new JPAEntityListTypeHandler(storage,
                        messageHandler,
                        bidirectionalHelpDialogTitle,
                        fieldInitializer,
                        initialQueryTextGenerator));
    }

    public JPAEntityListFieldHandler(EntityManager entityManager,
            MessageHandler messageHandler,
            TypeHandler<List<Object>, FieldUpdateEvent<List<Object>>,JPAReflectionFormBuilder, AbstractListPanel> typeHandler) {
        super(messageHandler, typeHandler);
    }
}

