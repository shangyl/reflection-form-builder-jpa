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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.metamodel.Metamodel;
import javax.swing.BorderFactory;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.LayoutStyle;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import richtercloud.reflection.form.builder.jpa.HistoryEntry;

/**
 * A component which provides a text field to enter JPQL queries for a
 * pre-defined class, a spinner to control the length the result set and
 * methods to retrieve the query result to be used in a component which
 * visualizes it.
 *
 * @author richter
 */
public class QueryComponent<E> extends JPanel {
    private final static Logger LOGGER = LoggerFactory.getLogger(QueryComponent.class);
    /**
     * the default value for the initial query limit (see {@link #QueryPanel(javax.persistence.EntityManager, java.lang.Class, int) } for details
     */
    public static final int INITIAL_QUERY_LIMIT_DEFAULT = 20;
    private final static Comparator<HistoryEntry> QUERY_HISTORY_COMPARATOR_USAGE = new Comparator<HistoryEntry>() {
        @Override
        public int compare(HistoryEntry o1, HistoryEntry o2) {
            return Integer.compare(o1.getUsageCount(), o2.getUsageCount());
        }
    };
    private final static Comparator<HistoryEntry> QUERY_HISTORY_COMPARATOR_DATE = new Comparator<HistoryEntry>() {
        @Override
        public int compare(HistoryEntry o1, HistoryEntry o2) {
            return o1.getLastUsage().compareTo(o2.getLastUsage());
        }
    };
    /**
     * Creates a mutable list with one {@link HistoryEntry} to select all
     * entities of type {@code entityClass}.
     * @param entityClass
     * @return
     */
    public static List<HistoryEntry> generateInitialHistoryDefault(Class<?> entityClass) {
        List<HistoryEntry> retValue = new ArrayList<>(Arrays.asList(new HistoryEntry(createQueryText(entityClass), //queryText
                1, //usageCount
                new Date() //lastUsage
        )));
        return retValue;
    }
    public static String generateEntityClassQueryIdentifier(Class<?> entityClass) {
        String retValue = String.valueOf(Character.toLowerCase(entityClass.getSimpleName().charAt(0)));
        return retValue;
    }
    private static String createQueryText(Class<?> entityClass) {
        //Criteria API doesn't allow retrieval of string/text from objects
        //created with CriteriaBuilder, but text should be the first entry in
        //the query combobox -> construct String instead of using
        //CriteriaBuilder
        String entityClassQueryIdentifier = generateEntityClassQueryIdentifier(entityClass);
        String retValue = String.format("SELECT %s from %s %s",
                entityClassQueryIdentifier,
                entityClass.getSimpleName(),
                entityClassQueryIdentifier);
        return retValue;
    }
    public static void validateEntityClass(Class<?> entityClass, EntityManager entityManager) {
        Metamodel meta = entityManager.getMetamodel();
        try {
            meta.entity(entityClass);
        }catch(IllegalArgumentException ex) {
            throw new IllegalArgumentException(String.format("entityClass %s is not a mapped entity", entityClass), ex);
        }
    }
    private EntityManager entityManager;
    private Class<? extends E> entityClass;
    private final SpinnerModel queryLimitSpinnerModel = new SpinnerNumberModel(INITIAL_QUERY_LIMIT_DEFAULT, //value
            1, //min
            null, //max
            1 //stepSize
    );

    private final SortedComboBoxModel<HistoryEntry> queryComboBoxModel;
    private final QueryComboBoxEditor queryComboBoxEditor;
    /**
     * the {@code query} argument of the last execution of {@link #executeQuery(javax.persistence.TypedQuery, int, java.lang.String) }
     */
    private TypedQuery<? extends E> lastQuery;
    /**
     * the {@code queryLimit} arugment of the last execution of {@link #executeQuery(javax.persistence.TypedQuery, int, java.lang.String) }
     */
    private int lastQueryLimit;
    /**
     * the {@code queryText} argument of the last execution of {@link #executeQuery(javax.persistence.TypedQuery, int, java.lang.String) }
     */
    private String lastQueryText;
    private final JButton queryButton;
    private final JComboBox<HistoryEntry> queryComboBox;
    private final JLabel queryLabel;
    private final JLabel queryLimitLabel;
    private final JSpinner queryLimitSpinner;
    private final JTextArea queryStatusLabel;
    private final JScrollPane queryStatusLabelScrollPane;
    private Set<QueryComponentListener> listeners = new HashSet<>();

    public QueryComponent(EntityManager entityManager,
            Class<? extends E> entityClass) throws IllegalArgumentException, IllegalAccessException {
        this(entityManager,
                entityClass,
                generateInitialHistoryDefault(entityClass),
                null, //initialSelectedHistoryEntry (null means point to the first item of initialHistory
                INITIAL_QUERY_LIMIT_DEFAULT);
    }

    protected QueryComponent(EntityManager entityManager,
            Class<? extends E> entityClass,
            List<HistoryEntry> initialHistory,
            HistoryEntry initialSelectedHistoryEntry,
            int initialQueryLimit) throws IllegalArgumentException, IllegalAccessException {
        if(entityClass == null) {
            throw new IllegalArgumentException("entityClass mustn't be null");
        }
        queryLabel = new JLabel();
        queryButton = new JButton();
        queryComboBox = new JComboBox<>();
        queryLimitSpinner = new JSpinner();
        queryLimitLabel = new JLabel();
        queryStatusLabelScrollPane = new JScrollPane();
        queryStatusLabel = new JTextArea();
        validateEntityClass(entityClass, entityManager);
        this.entityClass = entityClass;
        //initialize with initial item in order to minimize trouble with null
        //being set as editor item in JComboBox.setEditor
        this.queryComboBoxModel = new SortedComboBoxModel<>(QUERY_HISTORY_COMPARATOR_USAGE, new LinkedList<>(initialHistory));
        this.queryComboBoxEditor = new QueryComboBoxEditor(entityClass);
                //before initComponents because it's used there (yet sets item
                //of editor to null, so statement after initComponent is
                //necessary
        this.initComponents();
        if(initialSelectedHistoryEntry != null) {
            if(!initialHistory.contains(initialSelectedHistoryEntry)) {
                throw new IllegalArgumentException("if initialSelectedHistoryEntry is != null it has to be contained in initialHistory");
            }
            this.queryComboBox.setSelectedItem(initialSelectedHistoryEntry);
        } else {
            if(!initialHistory.isEmpty()) {
                this.queryComboBox.setSelectedItem(initialHistory.get(0));
            }else {
                this.queryComboBox.setSelectedItem(null);
            }
        }
        this.entityManager = entityManager;
        this.queryLabel.setText(String.format("%s query:", entityClass.getSimpleName()));
        String queryText = createQueryText(entityClass);
        TypedQuery<? extends E> query = entityManager.createQuery(queryText, entityClass);
        this.executeQuery(query, initialQueryLimit, queryText);
    }

    public List<HistoryEntry> getQueryHistory() {
        return new LinkedList<>(this.getQueryComboBoxModel().getItems());
    }

    /**
     * @return the queryComboBoxModel
     */
    /*
    internal implementation notes:
    - expose in order to be able to reuse/update queries
    */
    public SortedComboBoxModel<HistoryEntry> getQueryComboBoxModel() {
        return queryComboBoxModel;
    }

    private void initComponents() {

        queryLabel.setText("Query:");

        queryButton.setText("Run query");
        queryButton.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                queryButtonActionPerformed(evt);
            }
        });

        queryComboBox.setEditable(true);
        queryComboBox.setModel(queryComboBoxModel);
        queryComboBox.setEditor(queryComboBoxEditor);

        queryLimitSpinner.setModel(queryLimitSpinnerModel);
        queryLimitSpinner.setValue(INITIAL_QUERY_LIMIT_DEFAULT);

        queryLimitLabel.setText("# of Results");

        queryStatusLabelScrollPane.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));

        queryStatusLabel.setEditable(false);
        queryStatusLabel.setColumns(20);
        queryStatusLabel.setLineWrap(true);
        queryStatusLabel.setRows(2);
        queryStatusLabel.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
        queryStatusLabelScrollPane.setViewportView(queryStatusLabel);

        GroupLayout layout = new GroupLayout(this);
        this.setLayout(layout);
        GroupLayout.ParallelGroup horizontalParallelGroup = layout.createParallelGroup(GroupLayout.Alignment.LEADING);
        horizontalParallelGroup.addGroup(layout.createSequentialGroup()
                        .addComponent(queryLabel)
                        .addGap(18, 18, 18)
                        .addComponent(queryComboBox, 0, 283, Short.MAX_VALUE)
                        .addGap(18, 18, 18)
                        .addComponent(queryLimitLabel)
                        .addGap(18, 18, 18)
                        .addComponent(queryLimitSpinner, GroupLayout.DEFAULT_SIZE, 179, Short.MAX_VALUE)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(queryButton))
                    .addComponent(queryStatusLabelScrollPane, GroupLayout.Alignment.TRAILING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE));

        layout.setHorizontalGroup(
            layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(horizontalParallelGroup)
                .addContainerGap())
        );

        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(queryLabel)
                    .addComponent(queryButton)
                    .addComponent(queryComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(queryLimitSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(queryLimitLabel))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(queryStatusLabelScrollPane, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)));
    }

    private void queryButtonActionPerformed(java.awt.event.ActionEvent evt) {
        //although queryComboBox's model should never be empty in the current
        //implementation, there's check nevertheless so that future changes
        //don't cause too much trouble (adding a function to delete history
        //makes sense)
        String queryText;
        //there's no good way to tell if the ComboBox is currently being edited
        //(the JComboBox doesn't know and thus doesn't change the selected item
        //and selected index property and the editor can't tell because it
        //doesn't know the model)
        HistoryEntry queryComboBoxEditorItem = queryComboBoxEditor.getItem();
        if(queryComboBoxEditorItem != null && !queryComboBoxModel.contains(queryComboBoxEditorItem)) {
            queryText = queryComboBoxEditorItem.getText();
            if(queryText == null || queryText.isEmpty()) {
                queryStatusLabel.setText("Enter a query");
                return;
            }
        }else if(queryComboBox.getSelectedIndex() >= 0) {
            HistoryEntry selectedHistoryEntry = this.queryComboBox.getItemAt(this.queryComboBox.getSelectedIndex());
            queryText = selectedHistoryEntry.getText();
        }else {
            this.queryStatusLabel.setText("No query entered or selected");
            return;
        }
        int queryLimit = (int) queryLimitSpinner.getValue();
        TypedQuery<? extends E> query = this.createQuery(queryText); //handles displaying
            //exceptions which occured during query execution (explaining
            //syntax errors)
        if(query != null) {
            this.executeQuery(query, queryLimit, queryText);
        }
    }

    public void addListener(QueryComponentListener listener) {
        this.listeners.add(listener);
    }

    public void removeListener(QueryComponentListener listener) {
        this.listeners.remove(listener);
    }

    /**
     * Executes JQPL query {@code query}. Creates a {@link HistoryEntry} in the
     * {@code queryComboBoxModel} and resets the current item of
     * {@code queryComboBoxEditor}.
     * @param query
     */
    /*
    internal implementation notes:
    - in order to produce HistoryEntrys from every query it's necessary to pass
    the text of the query because there's no way to retrieve text from Criteria
    objects
    */
    private void executeQuery(TypedQuery<? extends E> query,
            int queryLimit,
            String queryText) {
        LOGGER.debug("executing query '{}'", queryText);
        try {
            List<? extends E> queryResultTmp = query.setMaxResults(queryLimit).getResultList();
            for(QueryComponentListener listener : listeners) {
                listener.onQueryExecuted(new QueryComponentEvent(queryResultTmp));
            }
            this.queryStatusLabel.setText("Query executed successfully.");
            HistoryEntry entry = queryComboBoxEditor.getItem();
            if(entry == null) {
                //if the query came from a HistoryEntry from the combo box model
                entry = new HistoryEntry(queryText, 1, new Date());
            }
            if(!this.queryComboBoxModel.contains(entry)) {
                this.getQueryComboBoxModel().addElement(entry);
            }
            this.queryComboBoxEditor.setItem(null); //reset to indicate the need
                //to create a new item
        }catch(Exception ex) {
            LOGGER.info("an exception occured while executing the query", ex);
            this.queryStatusLabel.setText(generateStatusMessage(ex.getMessage()));
        }
        this.lastQuery = query;
        this.lastQueryLimit = queryLimit;
        this.lastQueryText = queryText;
    }

    public void repeatLastQuery() {
        executeQuery(lastQuery, lastQueryLimit, lastQueryText);
    }

    private TypedQuery<? extends E> createQuery(String queryText) {
        try {
            TypedQuery<? extends E> query = this.entityManager.createQuery(queryText, this.entityClass);
            return query;
        }catch(Exception ex) {
            LOGGER.info("an exception occured while executing the query", ex);
            this.queryStatusLabel.setText(generateStatusMessage(ex.getMessage()));
        }
        return null;
    }

    public JTextArea getQueryStatusLabel() {
        return queryStatusLabel;
    }

    public Class<? extends E> getEntityClass() {
        return entityClass;
    }

    /**
     * Allows later changes to message generation depending on the mechanism
     * used for displaying (label (might require {@code <html></html>} tags around message), textarea, dialog, etc.)
     *
     * @param message
     * @return the generated status message
     */
    private String generateStatusMessage(String message) {
        return message;
    }
}