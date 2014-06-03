package just4fun.android.demo1.test;

import android.os.Bundle;
import android.test.InstrumentationTestRunner;
import android.util.Log;


public class XInstrumentation extends InstrumentationTestRunner {

public XInstrumentation() { }


@Override public void onStart() {
	super.onStart();
	Log.w("XInstrumentation", "__________________________________ START");
}
@Override public void onDestroy() {
	Log.w("XInstrumentation", "__________________________________ DESTROY");
	super.onDestroy();
}
@Override public boolean onException(Object obj, Throwable e) {
	Log.w("XInstrumentation", "__________________________________ EXCEPTION");
	return super.onException(obj, e);
}
@Override public void onCreate(Bundle arguments) {
	super.onCreate(arguments);
	Log.w("XInstrumentation", "__________________________________ CREATE");
}
}
