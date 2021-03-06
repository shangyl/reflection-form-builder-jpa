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
package de.richtercloud.reflection.form.builder.jpa.storage;

import de.richtercloud.reflection.form.builder.storage.StorageConf;
import de.richtercloud.reflection.form.builder.storage.StorageConfValidationException;
import java.io.EOFException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.apache.commons.lang3.builder.RecursiveToStringStyle;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * Holds configuration parameter for file-based database persistence storage as
 * well as scheme validation routines.
 *
 * Since database names in derby refer to base directories (and simple names
 * without path separator to the working directory of the database server (in
 * network mode) or the JVM (in embedded mode)), there's only a database
 * name option to configure (name is chosen instead of directory in order to
 * make the property reusable for other databases, like PostgreSQL).
 *
 * @author richter
 */
public abstract class AbstractPersistenceStorageConf implements StorageConf, Serializable {
    private static final long serialVersionUID = 1L;
    public final static String PASSWORD_DEFAULT = "";
    /**
     * Embedded Derby doesn't need a non-empty username, but the network
     * connection does.
     */
    private String username;
    private String password = PASSWORD_DEFAULT;
    /**
     * Can refer to name or directory depending on the implementation (see class
     * comment for more info).
     */
    private String databaseName;
    private File schemeChecksumFile;
    private Set<Class<?>> entityClasses;
    /**
     * The name of the database driver. Not configurable because configuration
     * classes are bound to one connection type (represented by a driver).
     */
    private final String databaseDriver;

    /**
     * Generates a checksum to track changes to {@code clazz} from the hash codes of declared fields and methods (tracking both might cause redundancies, but increases safety of getting all changes of database relevant properties).
     *
     * This could be used to generate {@code serialVersionUID}, but shouldn't be necessary.
     *
     * Doesn't care about constructors since they have no influence on database schemes.
     *
     * @param clazz the class to generate for
     * @return the generated checksum
     */
    public static long generateSchemeChecksum(Class<?> clazz) {
        long retValue = 0L;
        for(Field field : clazz.getDeclaredFields()) {
            retValue += field.hashCode();
        }
        for(Method method : clazz.getDeclaredMethods()) {
            retValue += method.hashCode();
        }
        return retValue;
    }

    private static Map<Class<?>, Long> generateSchemeChecksumMap(Set<Class<?>> classes) {
        Map<Class<?>, Long> retValue = new HashMap<>();
        for(Class<?> clazz: classes) {
            long checksum = generateSchemeChecksum(clazz);
            retValue.put(clazz, checksum);
        }
        return retValue;
    }

    public AbstractPersistenceStorageConf(String databaseDriver,
            Set<Class<?>> entityClasses,
            String username,
            String databaseDir,
            File schemeChecksumFile) throws FileNotFoundException, IOException {
        this(databaseDriver,
                entityClasses,
                username,
                null, //password
                databaseDir,
                schemeChecksumFile);
    }

    /**
     * Copy constructor.
     * @param databaseDriver the database driver
     * @param entityClasses the entity classes to manage
     * @param username the username
     * @param password the password
     * @param databaseName the database name
     * @param schemeChecksumFile the scheme checksum file
     * @throws FileNotFoundException in case the scheme checksum file can't be
     *     found
     * @throws IOException in case an I/O exception occurs while reading from or
     *     writing to the scheme checksum file
     */
    public AbstractPersistenceStorageConf(String databaseDriver,
            Set<Class<?>> entityClasses,
            String username,
            String password,
            String databaseName,
            File schemeChecksumFile) throws FileNotFoundException, IOException {
        this.databaseDriver = databaseDriver;
        this.entityClasses = entityClasses;
        this.username = username;
        this.password = password;
        this.databaseName = databaseName;
        this.schemeChecksumFile = schemeChecksumFile;
    }

    public String getDatabaseName() {
        return databaseName;
    }

    public void setDatabaseName(String databaseName) {
        this.databaseName = databaseName;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public abstract String getConnectionURL();

    public String getDatabaseDriver() {
        return databaseDriver;
    }

    public Set<Class<?>> getEntityClasses() {
        return entityClasses;
    }

    public void setEntityClasses(Set<Class<?>> entityClasses) {
        this.entityClasses = entityClasses;
    }

    public File getSchemeChecksumFile() {
        return schemeChecksumFile;
    }

    public void setSchemeChecksumFile(File schemeChecksumFile) {
        this.schemeChecksumFile = schemeChecksumFile;
    }

    /*
    internal implementation notes:
    - Metamodel implementations don't reliably implement `equals` (e.g.
    `org.hibernate.jpa.internal.metamodel.Metamodel` doesn't
    - java.lang.reflect.Field can't be serialized with `ObjectOutputStream`
    (fails with `java.io.NotSerializableException: java.lang.reflect.Field`) ->
    use version field, e.g. `serialVersionUID`
    obsolete internal implementation notes:
    - Metamodel can't be serialized with XMLEncoder because implementations
    don't guarantee to be persistable with it (needs a default constructor and
    also hibernate's MetamodelImpl doesn't provide one) -> ObjectOutputStream
    and ObjectInputStream
    */
    /**
     * Retrieves a persisted version of the database scheme (stored in
     * {@code lastSchemeStorageFile} and fails the validation if it doesn't
     * match with the current set of classes.
     */
    @Override
    @SuppressWarnings("PMD.EmptyCatchBlock")
    public void validate() throws StorageConfValidationException {
        if(this.databaseName == null) {
            throw new StorageConfValidationException("Database name isn't specified");
        }
        if(!schemeChecksumFile.exists()) {
            try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(Files.newOutputStream(schemeChecksumFile.toPath()))) {
                Map<Class<?>, Long> checksumMap = generateSchemeChecksumMap(entityClasses);
                objectOutputStream.writeObject(checksumMap);
                objectOutputStream.flush();
            } catch (IOException ex) {
                throw new StorageConfValidationException(ex);
            }
        }else {
            try {
                ObjectInputStream objectInputStream = new ObjectInputStream(Files.newInputStream(schemeChecksumFile.toPath()));
                Map<Class<?>, Long> checksumMapOld = (Map<Class<?>, Long>) objectInputStream.readObject();
                Map<Class<?>, Long> checksumMap = generateSchemeChecksumMap(entityClasses);
                if(!checksumMap.equals(checksumMapOld)) {
                    throw new StorageConfValidationException(String.format(
                            "The sum of checksum of class fields and methods "
                                    + "doesn't match with the persisted map in "
                                    + "'%s'. The indicates a change to the "
                                    + "metamodel and the database scheme needs "
                                    + "to be adjusted externally. It might "
                                    + "help to store the entities in an XML "
                                    + "file, open the XML file and store the "
                                    + "entities in the new format. If you're "
                                    + "sure you know what you're doing, "
                                    + "consider removing the old scheme "
                                    + "checksum file '%s' and restart the "
                                    + "application.",
                            this.schemeChecksumFile.getAbsolutePath(),
                            this.schemeChecksumFile.getAbsolutePath()));
                }
            } catch(EOFException ex) {
                //empty file for scheme checksum has been specified -> skip
                //check without complaining
            }catch (IOException | ClassNotFoundException ex) {
                throw new StorageConfValidationException(ex);
            }
        }
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 67 * hash + Objects.hashCode(this.username);
        hash = 67 * hash + Objects.hashCode(this.password);
        hash = 67 * hash + Objects.hashCode(this.databaseName);
        hash = 67 * hash + Objects.hashCode(this.schemeChecksumFile);
        hash = 67 * hash + Objects.hashCode(this.entityClasses);
        hash = 67 * hash + Objects.hashCode(this.databaseDriver);
        return hash;
    }

    protected boolean equalsTransitive(AbstractPersistenceStorageConf other) {
        if (!Objects.equals(this.username, other.username)) {
            return false;
        }
        if (!Objects.equals(this.password, other.password)) {
            return false;
        }
        if (!Objects.equals(this.databaseName, other.databaseName)) {
            return false;
        }
        if (!Objects.equals(this.databaseDriver, other.databaseDriver)) {
            return false;
        }
        if (!Objects.equals(this.schemeChecksumFile, other.schemeChecksumFile)) {
            return false;
        }
        return Objects.equals(this.entityClasses, other.entityClasses);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final AbstractPersistenceStorageConf other = (AbstractPersistenceStorageConf) obj;
        return equalsTransitive(other);
    }

    @Override
    public String toString() {
        ToStringBuilder toStringBuilder = new ReflectionToStringBuilder(this,
                new RecursiveToStringStyle());
        return toStringBuilder.toString();
    }
}
