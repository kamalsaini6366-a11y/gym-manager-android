package com.example.gymmanagement;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {
    DB db;
    LinearLayout root, content;
    TextView summary;
    EditText searchBox;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = new DB();
        buildShell();
        showDashboard();
    }

    int dp(int v) { return (int)(v * getResources().getDisplayMetrics().density); }

    TextView text(String s, int size, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s); t.setTextSize(size); t.setTextColor(0xFF202124);
        if (bold) t.setTypeface(null, Typeface.BOLD);
        t.setPadding(dp(4), dp(6), dp(4), dp(6));
        return t;
    }

    Button button(String s, Runnable action) {
        Button b = new Button(this); b.setText(s);
        b.setOnClickListener(v -> action.run());
        return b;
    }

    EditText edit(String hint) {
        EditText e = new EditText(this);
        e.setSingleLine(true); e.setHint(hint);
        e.setPadding(dp(12), dp(8), dp(12), dp(8));
        return e;
    }

    void buildShell() {
        ScrollView scroll = new ScrollView(this);
        root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(12), dp(16), dp(20));
        root.setBackgroundColor(0xFFF7F8FA);
        scroll.addView(root);
        setContentView(scroll);
        TextView title = text("GYM MANAGER", 24, true);
        title.setTextColor(0xFF145DA0);
        root.addView(title);
        root.addView(text("Offline • Data stays on this phone", 13, false));
        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.VERTICAL);
        nav.addView(button("Dashboard", this::showDashboard));
        nav.addView(button("Members", this::showMembers));
        nav.addView(button("Attendance", this::showAttendance));
        nav.addView(button("Fees", this::showFees));
        nav.addView(button("Measurements", this::showMeasurements));
        root.addView(nav);
        View line = new View(this); line.setBackgroundColor(0xFFDADCE0);
        root.addView(line, new LinearLayout.LayoutParams(-1, dp(1)));
        content = new LinearLayout(this); content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(0, dp(10), 0, 0); root.addView(content);
    }

    void clear() { content.removeAllViews(); }
    String today() { return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date()); }

    void showDashboard() {
        clear();
        int total = db.count("members", null);
        int active = db.count("members", "active=1");
        int present = db.count("attendance", "att_date='" + today() + "'");
        content.addView(text("Dashboard", 21, true));
        content.addView(text("Total members: " + total, 17, true));
        content.addView(text("Active members: " + active, 17, true));
        content.addView(text("Attendance today: " + present, 17, true));
        content.addView(text("Quick actions", 18, true));
        content.addView(button("＋ Add Member", this::addMemberDialog));
        content.addView(button("✓ Mark Attendance", this::showAttendance));
        content.addView(text("This first version stores records locally on this device. Back up your phone regularly; uninstalling the app may remove its data.", 13, false));
    }

    void showMembers() {
        clear();
        content.addView(text("Members", 21, true));
        content.addView(button("＋ Add Member", this::addMemberDialog));
        searchBox = edit("Search name or phone");
        content.addView(searchBox);
        content.addView(button("Search", () -> listMembers(searchBox.getText().toString().trim())));
        listMembers("");
    }

    void listMembers(String query) {
        // Remove prior result views but retain title/add/search controls.
        while (content.getChildCount() > 4) content.removeViewAt(4);
        Cursor c;
        if (query.isEmpty()) {
            c = db.getReadableDatabase().rawQuery("SELECT * FROM members ORDER BY name COLLATE NOCASE", null);
        } else {
            c = db.getReadableDatabase().rawQuery("SELECT * FROM members WHERE name LIKE ? OR phone LIKE ? ORDER BY name COLLATE NOCASE",
                new String[]{"%" + query + "%", "%" + query + "%"});
        }
        if (c.getCount() == 0) content.addView(text("No members found.", 14, false));
        while (c.moveToNext()) {
            int id=c.getInt(c.getColumnIndexOrThrow("_id"));
            String name=c.getString(c.getColumnIndexOrThrow("name"));
            String phone=c.getString(c.getColumnIndexOrThrow("phone"));
            String plan=c.getString(c.getColumnIndexOrThrow("plan"));
            String expiry=c.getString(c.getColumnIndexOrThrow("expiry"));
            int active=c.getInt(c.getColumnIndexOrThrow("active"));
            content.addView(text(name + (active==1 ? " • Active" : " • Inactive") +
                "\nID: "+id+" | Phone: "+phone+"\nPlan: "+plan+" | Valid until: "+expiry, 14, false));
            LinearLayout actions = new LinearLayout(this);
            actions.addView(button("Attendance", () -> markAttendance(id, name)));
            actions.addView(button("Fee", () -> addFeeDialog(id, name)));
            actions.addView(button("Edit", () -> editMemberDialog(id)));
            content.addView(actions);
            View line = new View(this); line.setBackgroundColor(0xFFDADCE0);
            content.addView(line, new LinearLayout.LayoutParams(-1, dp(1)));
        }
        c.close();
    }

    void addMemberDialog() {
        LinearLayout form = new LinearLayout(this); form.setOrientation(LinearLayout.VERTICAL);
        EditText name=edit("Member name *"), phone=edit("Phone"), plan=edit("Membership plan (Monthly etc.)");
        EditText fee=edit("Plan fee (₹)"), joining=edit("Joining date YYYY-MM-DD (optional)"), expiry=edit("Valid until YYYY-MM-DD");
        form.addView(name); form.addView(phone); form.addView(plan); form.addView(fee); form.addView(joining); form.addView(expiry);
        new AlertDialog.Builder(this).setTitle("Add member").setView(form)
            .setNegativeButton("Cancel", null).setPositiveButton("Save", (d,w) -> {
                if (name.getText().toString().trim().isEmpty()) { toast("Member name is required"); return; }
                ContentValues v = new ContentValues();
                v.put("name", name.getText().toString().trim()); v.put("phone", phone.getText().toString().trim());
                v.put("plan", plan.getText().toString().trim()); v.put("fee", number(fee.getText().toString()));
                v.put("joining", joining.getText().toString().trim().isEmpty()?today():joining.getText().toString().trim());
                v.put("expiry", expiry.getText().toString().trim()); v.put("active", 1);
                db.getWritableDatabase().insert("members", null, v);
                toast("Member saved"); showMembers();
            }).show();
    }

    void editMemberDialog(int id) {
        Cursor c=db.getReadableDatabase().rawQuery("SELECT * FROM members WHERE _id=?", new String[]{String.valueOf(id)});
        if(!c.moveToFirst()){c.close();return;}
        String oldName=c.getString(c.getColumnIndexOrThrow("name"));
        String oldPhone=c.getString(c.getColumnIndexOrThrow("phone"));
        String oldPlan=c.getString(c.getColumnIndexOrThrow("plan"));
        String oldExpiry=c.getString(c.getColumnIndexOrThrow("expiry"));
        int oldActive=c.getInt(c.getColumnIndexOrThrow("active")); c.close();
        LinearLayout form=new LinearLayout(this); form.setOrientation(LinearLayout.VERTICAL);
        EditText name=edit("Name"); name.setText(oldName);
        EditText phone=edit("Phone"); phone.setText(oldPhone);
        EditText plan=edit("Plan"); plan.setText(oldPlan);
        EditText expiry=edit("Valid until YYYY-MM-DD"); expiry.setText(oldExpiry);
        CheckBox active=new CheckBox(this); active.setText("Active membership"); active.setChecked(oldActive==1);
        form.addView(name);form.addView(phone);form.addView(plan);form.addView(expiry);form.addView(active);
        new AlertDialog.Builder(this).setTitle("Edit member").setView(form)
          .setNegativeButton("Cancel",null).setPositiveButton("Save",(d,w)->{
            ContentValues v=new ContentValues();v.put("name",name.getText().toString().trim());
            v.put("phone",phone.getText().toString().trim());v.put("plan",plan.getText().toString().trim());
            v.put("expiry",expiry.getText().toString().trim());v.put("active",active.isChecked()?1:0);
            db.getWritableDatabase().update("members",v,"_id=?",new String[]{String.valueOf(id)});
            showMembers();
          }).show();
    }

    void showAttendance() {
        clear(); content.addView(text("Attendance",21,true));
        content.addView(text("Today's date: "+today(),14,false));
        EditText q=edit("Search member name or phone");
        content.addView(q);
        content.addView(button("Find members", () -> listAttendanceChoices(q.getText().toString().trim())));
        listAttendanceChoices("");
    }

    void listAttendanceChoices(String query) {
        while(content.getChildCount()>3) content.removeViewAt(3);
        Cursor c;
        if(query.isEmpty()) c=db.getReadableDatabase().rawQuery("SELECT _id,name,phone FROM members WHERE active=1 ORDER BY name",null);
        else c=db.getReadableDatabase().rawQuery("SELECT _id,name,phone FROM members WHERE active=1 AND (name LIKE ? OR phone LIKE ?) ORDER BY name",
             new String[]{"%"+query+"%","%"+query+"%"});
        while(c.moveToNext()){
            int id=c.getInt(0);String name=c.getString(1);String phone=c.getString(2);
            content.addView(text(name+" • "+phone,15,true));
            content.addView(button("Mark present",()->markAttendance(id,name)));
        }
        c.close();
        content.addView(text("Present today: "+db.count("attendance","att_date='"+today()+"'"),14,true));
    }

    void markAttendance(int memberId,String name) {
        Cursor c=db.getReadableDatabase().rawQuery("SELECT _id FROM attendance WHERE member_id=? AND att_date=?",
            new String[]{String.valueOf(memberId),today()});
        boolean exists=c.moveToFirst(); c.close();
        if(exists){toast(name+" is already marked present today");return;}
        ContentValues v=new ContentValues();v.put("member_id",memberId);v.put("att_date",today());
        v.put("att_time",new SimpleDateFormat("HH:mm",Locale.US).format(new Date()));
        db.getWritableDatabase().insert("attendance",null,v);
        toast("Attendance marked for "+name);
        if(content.getChildCount()>0) showAttendance();
    }

    void showFees() {
        clear();content.addView(text("Fee records",21,true));
        content.addView(button("＋ Record fee payment",()->{
            Cursor c=db.getReadableDatabase().rawQuery("SELECT _id,name FROM members ORDER BY name",null);
            if(c.getCount()==0){c.close();toast("Add a member first");return;}
            String[] names=new String[c.getCount()];int[] ids=new int[c.getCount()];int i=0;
            while(c.moveToNext()){ids[i]=c.getInt(0);names[i]=c.getString(1);i++;}c.close();
            new AlertDialog.Builder(this).setTitle("Select member").setItems(names,(d,which)->addFeeDialog(ids[which],names[which])).show();
        }));
        Cursor c=db.getReadableDatabase().rawQuery("SELECT fees._id,members.name,fees.amount,fees.pay_date,fees.note FROM fees LEFT JOIN members ON members._id=fees.member_id ORDER BY fees._id DESC",null);
        if(c.getCount()==0)content.addView(text("No fee payments recorded yet.",14,false));
        while(c.moveToNext()) content.addView(text(c.getString(1)+" • ₹"+c.getString(2)+"\nDate: "+c.getString(3)+" • "+c.getString(4),14,false));
        c.close();
    }

    void addFeeDialog(int memberId,String memberName) {
        LinearLayout form=new LinearLayout(this);form.setOrientation(LinearLayout.VERTICAL);
        EditText amount=edit("Amount paid (₹) *");EditText date=edit("Payment date YYYY-MM-DD");date.setText(today());
        EditText note=edit("Note / plan / GST details");
        form.addView(amount);form.addView(date);form.addView(note);
        new AlertDialog.Builder(this).setTitle("Fee payment: "+memberName).setView(form)
          .setNegativeButton("Cancel",null).setPositiveButton("Save",(d,w)->{
            if(amount.getText().toString().trim().isEmpty()){toast("Enter amount");return;}
            ContentValues v=new ContentValues();v.put("member_id",memberId);v.put("amount",number(amount.getText().toString()));
            v.put("pay_date",date.getText().toString().trim());v.put("note",note.getText().toString().trim());
            db.getWritableDatabase().insert("fees",null,v);toast("Fee payment saved");showFees();
          }).show();
    }

    void showMeasurements() {
        clear();content.addView(text("Measurements",21,true));
        content.addView(text("Record height, weight, biceps, triceps and shoulders for progress tracking.",14,false));
        content.addView(button("＋ Add measurement",()->{
            Cursor c=db.getReadableDatabase().rawQuery("SELECT _id,name FROM members ORDER BY name",null);
            if(c.getCount()==0){c.close();toast("Add a member first");return;}
            String[] names=new String[c.getCount()];int[] ids=new int[c.getCount()];int i=0;
            while(c.moveToNext()){ids[i]=c.getInt(0);names[i]=c.getString(1);i++;}c.close();
            new AlertDialog.Builder(this).setTitle("Select member").setItems(names,(d,which)->measurementDialog(ids[which],names[which])).show();
        }));
        Cursor c=db.getReadableDatabase().rawQuery("SELECT measurements.measure_date,members.name,measurements.height,measurements.weight,measurements.biceps,measurements.triceps,measurements.shoulders FROM measurements JOIN members ON members._id=measurements.member_id ORDER BY measurements._id DESC",null);
        if(c.getCount()==0)content.addView(text("No measurements recorded yet.",14,false));
        while(c.moveToNext())content.addView(text(c.getString(1)+" • "+c.getString(0)+"\nHeight: "+c.getString(2)+" cm | Weight: "+c.getString(3)+" kg\nBiceps: "+c.getString(4)+" | Triceps: "+c.getString(5)+" | Shoulders: "+c.getString(6),14,false));
        c.close();
    }

    void measurementDialog(int memberId,String name) {
        LinearLayout form=new LinearLayout(this);form.setOrientation(LinearLayout.VERTICAL);
        EditText date=edit("Date YYYY-MM-DD");date.setText(today());
        EditText height=edit("Height (cm)"),weight=edit("Weight (kg)"),biceps=edit("Biceps (cm)"),triceps=edit("Triceps (cm)"),shoulders=edit("Shoulders (cm)");
        form.addView(date);form.addView(height);form.addView(weight);form.addView(biceps);form.addView(triceps);form.addView(shoulders);
        new AlertDialog.Builder(this).setTitle("Measurements: "+name).setView(form)
          .setNegativeButton("Cancel",null).setPositiveButton("Save",(d,w)->{
            ContentValues v=new ContentValues();v.put("member_id",memberId);v.put("measure_date",date.getText().toString().trim());
            v.put("height",height.getText().toString().trim());v.put("weight",weight.getText().toString().trim());
            v.put("biceps",biceps.getText().toString().trim());v.put("triceps",triceps.getText().toString().trim());v.put("shoulders",shoulders.getText().toString().trim());
            db.getWritableDatabase().insert("measurements",null,v);showMeasurements();
          }).show();
    }

    double number(String s){try{return Double.parseDouble(s.trim());}catch(Exception e){return 0;}}
    void toast(String s){Toast.makeText(this,s,Toast.LENGTH_SHORT).show();}

    class DB extends SQLiteOpenHelper {
        DB(){super(MainActivity.this,"gym_manager.db",null,1);}
        public void onCreate(SQLiteDatabase d){
            d.execSQL("CREATE TABLE members(_id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT NOT NULL,phone TEXT,plan TEXT,fee REAL,joining TEXT,expiry TEXT,active INTEGER DEFAULT 1)");
            d.execSQL("CREATE TABLE attendance(_id INTEGER PRIMARY KEY AUTOINCREMENT,member_id INTEGER,att_date TEXT,att_time TEXT,UNIQUE(member_id,att_date))");
            d.execSQL("CREATE TABLE fees(_id INTEGER PRIMARY KEY AUTOINCREMENT,member_id INTEGER,amount REAL,pay_date TEXT,note TEXT)");
            d.execSQL("CREATE TABLE measurements(_id INTEGER PRIMARY KEY AUTOINCREMENT,member_id INTEGER,measure_date TEXT,height TEXT,weight TEXT,biceps TEXT,triceps TEXT,shoulders TEXT)");
        }
        public void onUpgrade(SQLiteDatabase d,int oldV,int newV){}
        int count(String table,String where){
            Cursor c=getReadableDatabase().rawQuery("SELECT COUNT(*) FROM "+table+(where==null?"":" WHERE "+where),null);
            c.moveToFirst();int n=c.getInt(0);c.close();return n;
        }
    }
}
