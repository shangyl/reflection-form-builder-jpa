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
package richtercloud.reflection.form.builder.jpa.panels;

import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import javax.swing.ComboBoxEditor;
import javax.swing.JTextField;
import richtercloud.reflection.form.builder.jpa.HistoryEntry;

/**
 *
 * @author richter
 */
class QueryComboBoxEditor implements ComboBoxEditor {

    private final JTextField editorComponent = new JTextField();
    /**
     * the new item which is about to be created and not (yet) part of the
     * model of the {@link JComboBox} (can be retrieved with
     * {@link #getItem() }).
     */
    private HistoryEntry item;
    private final Set<ActionListener> actionListeners = new HashSet<>();

    /**
     *
     */
    /*
    internal implementation notes:
    - requires entityClass argument in order to create initial HistoryEntry
    -> remove if that causes trouble
     */
    QueryComboBoxEditor(Class<?> entityClass) {
        this.editorComponent.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                if (QueryComboBoxEditor.this.item == null) {
                    QueryComboBoxEditor.this.item = new HistoryEntry(QueryComboBoxEditor.this.editorComponent.getText(), //queryText
                    1, //usageCount
                    new Date() //lastUsage
                    );
                } else {
                    QueryComboBoxEditor.this.item.setText(QueryComboBoxEditor.this.editorComponent.getText());
                }
            }
        });
    }

    @Override
    public JTextField getEditorComponent() {
        return this.editorComponent;
    }

    @Override
    public void setItem(Object anObject) {
        assert anObject instanceof HistoryEntry;
        this.item = (HistoryEntry) anObject;
        if (this.item != null) {
            this.editorComponent.setText(this.item.getText());
        }
    }

    @Override
    public HistoryEntry getItem() {
        return this.item;
    }

    @Override
    public void selectAll() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    /**
     * Adds {@code l} to the set of notified {@link ActionListener}s.
     * Doesn't have any effect if {@code l} already is a registered
     * listener.
     * @param l
     */
    /*
    internal implementation notes:
    - seems to be called without any precaution if listener has been added
    before (throwing exception if listener is already registered thus
    doesn't make sense)
     */
    @Override
    public void addActionListener(ActionListener l) {
        this.actionListeners.add(l);
    }

    /**
     * Removes {@code l} from the set of notified {@link ActionListener}s.
     * Doesn't have any effect if {@code l} isn't a registered listener.
     * @param l
     */
    /*
    internal implementation notes:
    - seems to be called without any precaution if listener has been added
    before (throwing exception if listener isn't registered thus doesn't
    make sense)
     */
    @Override
    public void removeActionListener(ActionListener l) {
        this.actionListeners.remove(l);
    }

}
