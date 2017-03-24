package com.example.ompra.scribble;

import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.os.Binder;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.support.v7.widget.Toolbar;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.zxing.WriterException;

import androidmads.library.qrgenearator.QRGContents;
import androidmads.library.qrgenearator.QRGEncoder;

public class MainActivity extends AppCompatActivity {
    private RecyclerView user_recycler_view;
    private FirebaseRecyclerAdapter<User,UserViewHolder> firebaseRecyclerAdapter;
    private DatabaseReference mReference;
    private Context mContext;
    QRGEncoder qrgEncoder;
    String TAG = "GenerateQRCode";
    Bitmap bitmap;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        mContext = this;
        mReference = FirebaseDatabase.getInstance().getReference("users");
        user_recycler_view = (RecyclerView)findViewById(R.id.user_recycler_view);
        /*if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT)
        {
            user_recycler_view.setLayoutManager(new GridLayoutManager(mContext,2));
        }
        else
        {
            user_recycler_view.setLayoutManager(new GridLayoutManager(mContext,1));
        }*/
        Autofit layout = new Autofit(this,400);
        user_recycler_view.setLayoutManager(layout);
        setupAdapter();
        user_recycler_view.setAdapter(firebaseRecyclerAdapter);
        user_recycler_view.setNestedScrollingEnabled(false);
        FloatingActionButton add_user = (FloatingActionButton)findViewById(R.id.addUser);
        add_user.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final User user = new User();
                final String[] user_id = new String[2];
                LayoutInflater layoutInflater = LayoutInflater.from(mContext);
                View user_for_dialog_view = layoutInflater.inflate(R.layout.user_form_dialog,null);
                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(mContext);
                alertDialogBuilder.setTitle("Enter Details");
                alertDialogBuilder.setView(user_for_dialog_view);
                final EditText user_id_edit_text = (EditText)user_for_dialog_view.findViewById(R.id.user_id_edit_text);
                final EditText user_name_edit_text = (EditText)user_for_dialog_view.findViewById(R.id.user_name_edit_text);
                alertDialogBuilder.setCancelable(false).
                        setPositiveButton("Add", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                user_id[0] = user_id_edit_text.getText().toString();
                                user_id[1] = user_name_edit_text.getText().toString();
                                user.setUser_id(user_id[0]);
                                user.setUser_name(user_id[1]);
                                mReference.child(user_id[0]).setValue(user);
                            }
                        })
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        });
                AlertDialog alertDialog = alertDialogBuilder.create();
                alertDialog.show();
            }
        });

    }

    private void setupAdapter() {
        firebaseRecyclerAdapter = new FirebaseRecyclerAdapter<User, UserViewHolder>(
                User.class,R.layout.user_item,UserViewHolder.class,mReference) {
            @Override
            protected void populateViewHolder(UserViewHolder viewHolder, User model, int position) {
                viewHolder.user_name.setText(model.getUser_name());
                viewHolder.user_id.setText(model.getUser_id());
                WindowManager manager = (WindowManager)getSystemService(WINDOW_SERVICE);
                Display display = manager.getDefaultDisplay();
                Point point = new Point();
                display.getSize(point);
                int width = point.x;
                int height = point.y;
                int smallerDimension = width < height ? width : height;
                smallerDimension = smallerDimension * 3 / 4;
                qrgEncoder = new QRGEncoder(model.getUser_id(),null, QRGContents.Type.TEXT,smallerDimension);
                try {
                    bitmap = qrgEncoder.encodeAsBitmap();
                    viewHolder.user_qrCode.setImageBitmap(bitmap);
                } catch (WriterException e) {
                    Log.v(TAG, e.toString());
                }
            }
        };
    }

    public static class Autofit extends GridLayoutManager{
        private int mColumnWidth;
        private boolean mColumnWidthChanged = true;

        public Autofit(Context context, int spanCount) {
            super(context, spanCount);
            setColumnWidth(checkedColumnWidth(context,spanCount));
        }

        public Autofit(Context context, int spanCount, int orientation, boolean reverseLayout)
        {
            super(context, spanCount, orientation, reverseLayout);
            setColumnWidth(checkedColumnWidth(context,spanCount));
        }
        private int checkedColumnWidth(Context context, int columnWidth) {
            if (columnWidth <= 0)
            {
                columnWidth = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 48, context.getResources().getDisplayMetrics());
            }
            return columnWidth;
        }

        @Override
        public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state)
        {
            if(mColumnWidthChanged && mColumnWidth > 0)
            {
                int totalSpace;
                if (getOrientation() == VERTICAL) {
                    totalSpace = getWidth() - getPaddingRight() - getPaddingLeft();
                } else {
                    totalSpace = getHeight() - getPaddingTop() - getPaddingBottom();
                }
                int spanCount = Math.max(1, totalSpace / mColumnWidth);
                setSpanCount(spanCount);
                mColumnWidthChanged = false;
            }
            super.onLayoutChildren(recycler, state);
        }

        public void setColumnWidth(int width)
        {
            if(width>0 && width != mColumnWidth)
            {
                mColumnWidth = width;
                mColumnWidthChanged=true;
            }
        }
    }

    public static class UserViewHolder extends RecyclerView.ViewHolder {
        private TextView user_id;
        private TextView user_name;
        private ImageView user_qrCode;
        public UserViewHolder(View itemView) {
            super(itemView);

            user_name = (TextView)itemView.findViewById(R.id.user_name);
            user_id = (TextView)itemView.findViewById(R.id.user_id);
            user_qrCode = (ImageView)itemView.findViewById(R.id.user_qrCode);
        }

    }
}
