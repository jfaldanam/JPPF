/*
 * JPPF.
 * Copyright (C) 2005-2016 JPPF Team.
 * http://www.jppf.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jppf.ui.monitoring.diagnostics;

import java.awt.Color;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;

import org.jppf.client.monitoring.topology.*;
import org.jppf.management.*;
import org.jppf.management.diagnostics.*;
import org.jppf.ui.actions.EditorMouseListener;
import org.jppf.ui.monitoring.node.actions.AbstractTopologyAction;
import org.jppf.ui.options.factory.OptionsHandler;
import org.jppf.ui.utils.TreeTableUtils;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * This action displays the driver or node environment information in a separate frame.
 */
public class ThreadDumpAction extends AbstractTopologyAction {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(ThreadDumpAction.class);
  /**
   * Determines whether debug log statements are enabled.
   */
  private static boolean debugEnabled = log.isDebugEnabled();

  /**
   * Initialize this action.
   */
  public ThreadDumpAction() {
    setupIcon("/org/jppf/ui/resources/thread_dump.gif");
    setupNameAndTooltip("health.thread.dump");
  }

  @Override
  public void updateState(final List<Object> selectedElements) {
    this.selectedElements = selectedElements;
    dataArray = new AbstractTopologyComponent[selectedElements.size()];
    int count = 0;
    for (Object o: selectedElements) dataArray[count++] = (AbstractTopologyComponent) o;
    setEnabled(dataArray.length > 0);
  }

  @Override
  public void actionPerformed(final ActionEvent event) {
    try {
      final JDialog dialog = new JDialog(OptionsHandler.getMainWindow(), "Thread dump", false);
      dialog.setIconImage(((ImageIcon) getValue(SMALL_ICON)).getImage());
      dialog.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
      //frame.get
      dialog.addWindowListener(new WindowAdapter() {
        @Override
        public void windowClosing(final WindowEvent e) {
          dialog.setVisible(false);
          dialog.dispose();
        }
      });
      JEditorPane editor = new JEditorPane("text/html", "");
      editor.setBackground(Color.WHITE);
      editor.setText("retrieving thread dump information ...");
      editor.setCaretPosition(0);
      AbstractButton btn = (AbstractButton) event.getSource();
      if (btn.isShowing()) location = btn.getLocationOnScreen();
      editor.setEditable(false);
      editor.setOpaque(true);
      JScrollPane panel = new JScrollPane(editor);
      dialog.getContentPane().add(panel);
      AbstractAction escAction = new AbstractAction() {
        @Override
        public void actionPerformed(final ActionEvent event) {
          dialog.setVisible(false);
          dialog.dispose();
        }
      };
      setOkCancelKeys(panel, null, escAction);
      dialog.setLocationRelativeTo(null);
      dialog.setLocation(location);
      dialog.setSize(600, 600);
      dialog.setVisible(true);
      runAction(new AsyncRunnable(dialog, editor));
    } catch(Exception e) {
      if (debugEnabled) log.debug(e.getMessage(), e);
    }
  }

  /**
   * Retrieve the system information for the specified topology object.
   * @param data the topology object for which to get the information.
   * @return a {@link JPPFSystemInformation} or <code>null</code> if the information could not be retrieved.
   */
  private ThreadDump retrieveThreadDump(final AbstractTopologyComponent data) {
    ThreadDump info = null;
    try {
      if (data.isNode()) {
        TopologyDriver parent = (TopologyDriver) data.getParent();
        Map<String, Object> result = parent.getForwarder().threadDump(new UuidSelector(data.getUuid()));
        Object o = result.get(data.getUuid());
        if (o instanceof ThreadDump) info = (ThreadDump) o;
      }
      else info = ((TopologyDriver) data).getDiagnostics().threadDump();
    } catch (Exception e) {
      if (debugEnabled) log.debug(e.getMessage(), e);
    }
    return info;
  }

  /**
   * This class asynchronously retrieves the node or driver information and displays it int he dialog.
   */
  private class AsyncRunnable implements Runnable {
    /**
     * The dialog containing the editor component.
     */
    private final JDialog dialog;
    /**
     * The editor whose text is th node information.
     */
    private final JEditorPane editor;
    
    /**
     * Initialize this asynchronous task.
     * @param dialog the dialog containing the editor component.
     * @param editor the editor whose text is th node information.
     */
    public AsyncRunnable(final JDialog dialog, final JEditorPane editor) {
      this.dialog = dialog;
      this.editor = editor;
    }

    @Override
    public void run() {
      final StringBuilder html = new StringBuilder();
      final StringBuilder toClipboard = new StringBuilder();
      final StringBuilder title = new StringBuilder("Thread dump");
      try {
        ThreadDump info = retrieveThreadDump(dataArray[0]);
        boolean isNode = dataArray[0].isNode();
        title.append(" for ").append(isNode ? "node " : "driver ").append(TreeTableUtils.getDisplayName(dataArray[0]));
        if (info == null) html.append("<p><b>No thread dump was generated</b>");
        else {
          html.append(HTMLThreadDumpWriter.printToString(info, title.toString()));
          toClipboard.append(TextThreadDumpWriter.printToString(info, title.toString()));
        }
      } catch(Exception e) {
        toClipboard.append(ExceptionUtils.getStackTrace(e));
        html.append(toClipboard.toString().replace("\n", "<br>"));
      }
      SwingUtilities.invokeLater(new Runnable() {
        @Override
        public void run() {
          try {
          dialog.setTitle(title.toString());
          editor.setText(html.toString());
          editor.setCaretPosition(0);
          editor.addMouseListener(new EditorMouseListener(toClipboard.toString()));
          } catch(Exception e) {
            if (debugEnabled) log.debug("exception while setting thread dump dialog data: ", e);
          }
        }
      });
    }
  };
}
