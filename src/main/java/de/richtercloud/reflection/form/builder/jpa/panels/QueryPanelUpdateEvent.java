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
package de.richtercloud.reflection.form.builder.jpa.panels;

/**
 *
 * @author richter
 */
public class QueryPanelUpdateEvent {
    private final AbstractQueryPanel source;
    private final Object newSelectionItem;

    public QueryPanelUpdateEvent(Object newSelectionItem, AbstractQueryPanel source) {
        this.newSelectionItem = newSelectionItem;
        this.source = source;
    }

    public AbstractQueryPanel getSource() {
        return source;
    }

    public Object getNewSelectionItem() {
        return newSelectionItem;
    }
}
