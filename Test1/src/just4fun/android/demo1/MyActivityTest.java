package just4fun.android.demo1;

import android.app.Instrumentation;
import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;


/**
 This is a simple framework for a test of an Application.  See
 {@link android.test.ApplicationTestCase ApplicationTestCase} for more information on
 how to write and extend Application tests.
 <p/>
 To run this test, you can type:
 adb shell am instrument -w -e class just4fun.android.demo1.MyActivityTest  just4fun.android.demo1.tests/android.test.InstrumentationTestRunner
 adb shell am instrument -w \
 -e class just4fun.android.demo1.MyActivityTest \
 just4fun.android.demo1.tests/android.test.InstrumentationTestRunner
 */
public class MyActivityTest extends ActivityInstrumentationTestCase2<MyActivity> {
static final String TAG = "just4fun";
MyActivity activity;
Instrumentation instr;

public MyActivityTest() {
	super(MyActivity.class);
	tag("CONSTRUCTED");
}

@Override public void setUp() throws Exception {
	super.setUp();
	tag("SETUP");
	activity = getActivity();
	tag("APPLICATION "+activity.getApplication().hashCode()+";  ACTIVITY " +activity.hashCode());
	instr = getInstrumentation();
}
@Override public void tearDown() throws Exception {
	tag("TEAR DOWN");
	super.tearDown();
//	instr.new
}

//@UiThreadTest
public void test1() throws Exception {
	tag("TEST 1");
	sleep(5000);
	runInUi(new Runnable() {
		@Override public void run() { activity.finish(); }
	});
	sleep(8000);
}
public void test2() throws Exception {
	tag("TEST 2");
	sleep(2000);
	runInUi(new Runnable() {
		@Override public void run() { activity.finish(); }
	});
	sleep(8000);
}

void tag(String msg) {
	Log.w(TAG, "---------------------------------------------------------------------------------------------     "+msg);
}
void sleep(long delay) {
	try {Thread.sleep(delay);} catch (Exception ex) {Log.e(TAG, "", ex);}
}
void runInUi(Runnable r) {
	instr.runOnMainSync(r);
}

}
