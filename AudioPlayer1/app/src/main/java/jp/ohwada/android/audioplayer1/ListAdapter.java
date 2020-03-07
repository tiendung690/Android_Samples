/**
 *  Audio Player Sample
 * 2019-02-01 K.OHWADA 
 */

package jp.ohwada.android.audioplayer1;

import java.io.File;
import java.util.List;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;


/**
 * adapter for ListView
 */
public class ListAdapter extends ArrayAdapter<String>
{

	// dubug
	private  final static boolean D = true; 
	private  final static String TAG = "Audio";
	private  final static String TAG_SUB = "ListAdapter";

  	public final static int LAYOUT_RESOURCE_ID = R.layout.list_item;

	private LayoutInflater mInflater = null;

			
    /**
     * constractor  
     */
public  ListAdapter( Context context, int resource, List<String> objects ) {
		super( context, resource, objects );
		mInflater = (LayoutInflater) context.getSystemService( 
			Context.LAYOUT_INFLATER_SERVICE ) ;
	} // ListAdapter



/**
 * get view 
 */
@Override
public View getView( int position, View convertView, ViewGroup parent ) {
		//log_d("getView");
		View view = convertView;
        ListHolder h = null;  
            
		// once at first
		if ( view == null ) {
			// get view
			view = mInflater.inflate( LAYOUT_RESOURCE_ID, null );		
			// save 
			h = new ListHolder(); 
			h.tv_filename = (TextView) view.findViewById( R.id.TextView_item_filename );
			view.setTag( h ); 

		} else {
			// load  
			h = (ListHolder) view.getTag();  
		}  
     
		// get item form Adapter
		String item = (String) getItem( position );

		h.tv_filename.setText( item ) ;

		return view;

} // getView




/**
 * write into logcat
 */ 
private void log_d( String msg ) {
	    if (D) Log.d( TAG, TAG_SUB + " " + msg );
} // log_d


	/**
	 * class ListHolder
	 */	
	static class ListHolder { 
		public TextView tv_filename;
    } // class ListHolder

} // class ListAdapter
