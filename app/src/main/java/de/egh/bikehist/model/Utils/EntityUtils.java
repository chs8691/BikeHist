package de.egh.bikehist.model.Utils;

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

/**
 /** Utils for a particular Entity type.
 */

public interface EntityUtils<T> {

	/** Return the name - local independent - of the Entity in plural form (not of the instance!). */
	public String getEntityNamePlural();

	/** URI from BikeHistProvider.CONTENT_URI_ */
	public Uri getContentUri();

	public String toExport(T entity);

	/**
	 Create new entity for the actual data set in the cursor. Cursor must point to an entity of this
	 type.

	 @return Entity is null, if dataset is invalid
	 */
	public T build(Cursor c);

	/** Tag builder for SQL inserts. */
	public ContentValues build(T entity);
}

