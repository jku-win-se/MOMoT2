package org.moeaframework.util.progress;

import java.util.EventListener;

public interface ProgressListener extends EventListener {
   void progressUpdate(ProgressEvent event);
}
