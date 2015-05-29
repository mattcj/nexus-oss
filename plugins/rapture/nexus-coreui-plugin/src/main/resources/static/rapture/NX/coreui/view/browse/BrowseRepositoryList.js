/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-2015 Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
/*global Ext, NX*/

/**
 * Repository grid.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.view.browse.BrowseRepositoryList', {
  extend: 'NX.view.drilldown.Master',
  alias: 'widget.nx-coreui-browse-repository-list',
  requires: [
    'NX.I18n'
  ],

  config: {
    stateful: true,
    stateId: 'nx-coreui-browse-repository-list'
  },

  store: 'NX.coreui.store.Repository',

  columns: [
    {
      xtype: 'nx-iconcolumn',
      width: 36,
      iconVariant: 'x16',
      iconNamePrefix: 'repository-',
      dataIndex: 'type'
    },
    {header: NX.I18n.get('Browse_BrowseRepositoryList_Name_Column'), dataIndex: 'name', stateId: 'name', flex: 1},
    {header: NX.I18n.get('Browse_BrowseRepositoryList_Type_Column'), dataIndex: 'type', stateId: 'type'},
    {header: NX.I18n.get('Browse_BrowseRepositoryList_Format_Column'), dataIndex: 'format', stateId: 'format'}
  ],

  viewConfig: {
    emptyText: NX.I18n.get('Browse_BrowseRepositoryList_EmptyText_View'),
    deferEmptyText: false,
    markDirty: false
  },

  plugins: [
    {ptype: 'gridfilterbox', emptyText: NX.I18n.get('Browse_BrowseRepositoryList_EmptyText_Filter')}
  ]

});
