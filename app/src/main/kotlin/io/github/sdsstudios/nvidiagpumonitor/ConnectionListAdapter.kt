package io.github.sdsstudios.nvidiagpumonitor

import android.content.Context
import android.database.Cursor
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.MainThread
import androidx.cursoradapter.widget.CursorAdapter
import com.sonelli.juicessh.pluginlibrary.PluginContract.Connections
import io.github.getsixtyfour.openpyn.R
import java.util.UUID

/**
 * Created by Seth on 04/03/18.
 */
@MainThread
class ConnectionListAdapter(ctx: Context) : CursorAdapter(ctx, null, false) {

    private val mInflater = ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    override fun getItem(position: Int): Cursor? {
        return super.getItem(position) as Cursor?
    }

    override fun newView(context: Context, cursor: Cursor, parent: ViewGroup): View {
        return mInflater.inflate(R.layout.spinner_item, parent, false)
    }

    override fun newDropDownView(context: Context, cursor: Cursor, parent: ViewGroup): View {
        return mInflater.inflate(R.layout.spinner_dropdown_item, parent, false)
    }

    override fun bindView(view: View, context: Context, cursor: Cursor) {
        val columnIndex = cursor.getColumnIndex(Connections.COLUMN_NAME)

        if (columnIndex > -1) {
            view.findViewById<TextView>(R.id.text).text = cursor.getString(columnIndex)
        }
    }

    fun getConnectionId(position: Int): UUID? = getItem(position)?.run {
        val columnIndex = getColumnIndex(Connections.COLUMN_ID)

        if (columnIndex > -1) {
            UUID.fromString(getString(columnIndex))
        } else {
            null
        }
    }
}
