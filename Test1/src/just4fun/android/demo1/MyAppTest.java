package just4fun.android.demo1;

import android.app.Instrumentation;
import android.test.ActivityUnitTestCase;
import android.util.Log;


public class MyAppTest extends ActivityUnitTestCase<MyActivity> {
MyActivity cnActivity;

public MyAppTest() {
	super(MyActivity.class);
	System.out.println("WWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWW  1");
}

@Override public void setUp() throws Exception {
	super.setUp();
//	setApplication(new App());
	System.out.println("WWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWW  2");
	cnActivity = getActivity();
}
@Override public void tearDown() throws Exception {
	super.tearDown();
	System.out.println("WWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWW  3");
}

public void test1() throws Exception {
	Instrumentation mInstr = getInstrumentation();
	Log.w("MyActivity", "test1 WWWWWWWWW");
}
}
