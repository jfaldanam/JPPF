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

package org.jppf.admin.web.topology;

import javax.swing.tree.DefaultMutableTreeNode;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.Model;
import org.jppf.admin.web.tabletree.*;
import org.jppf.client.monitoring.AbstractComponent;
import org.jppf.client.monitoring.topology.AbstractTopologyComponent;

/**
 *
 * @author Laurent Cohen
 */
public class SelectAllLink extends AbstractSelectionLink {
  /**
   *
   */
  public SelectAllLink() {
    super(TopologyTree.SELECT_ALL_ACTION, Model.of("Select all"));
    imageName = "select_all.gif";
  }

  @Override
  protected void onClick(final AjaxRequestTarget target, final TableTreeData data) {
    DefaultMutableTreeNode root = (DefaultMutableTreeNode) data.getModel().getRoot();
    SelectionHandler handler = data.getSelectionHandler();
    handler.clearSelection();
    for (int i=0; i<root.getChildCount(); i++) {
      DefaultMutableTreeNode dmtnDriver = (DefaultMutableTreeNode) root.getChildAt(i);
      handler.select(((AbstractComponent<?>) dmtnDriver.getUserObject()).getUuid());
      for (int j=0; j<dmtnDriver.getChildCount(); j++) {
        DefaultMutableTreeNode dmtnNode = (DefaultMutableTreeNode) dmtnDriver.getChildAt(j);
        AbstractTopologyComponent node = (AbstractTopologyComponent) dmtnNode.getUserObject();
        if (node.isNode()) handler.select(node.getUuid());
      }
    }
  }
}