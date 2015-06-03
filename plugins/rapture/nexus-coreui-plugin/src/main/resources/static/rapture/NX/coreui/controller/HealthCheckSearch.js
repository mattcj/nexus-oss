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
 * HealthCheck search contribution controller.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.controller.HealthCheckSearch', {
  extend: 'Ext.app.Controller',
  requires: [
    'Ext.grid.column.Column',
    'NX.Conditions',
    'NX.util.Url',
    'NX.I18n'
  ],

  models: [
    'Asset',
    'Component'
  ],
  stores: [
    'Asset',
    'SearchResult'
  ],
  refs: [
    { ref: 'healthcheckResult', selector: 'nx-coreui-healthcheck-result-list' },
    { ref: 'componentDetails', selector: 'nx-coreui-component-details' }
  ],

  /**
   * @override
   */
  init: function() {
    var me = this;

    me.listen({
      store: {
        '#SearchResult': {
          prefetch: me.loadHealthCheckFields
        },
        '#BrowseResult': {
          prefetch: me.loadHealthCheckFields
        }
      },
      component: {
        'nx-coreui-healthcheck-result-list': {
          afterrender: me.bindHealthCheckColumns,
          selection: me.showHealthCheckInfo
        }
      }
    });
  },

  /**
   * @private
   */
  showHealthCheckInfo: function(grid, model) {
    var me = this,
        componentDetails = me.getComponentDetails(),
        info3 = {};

    if (!grid['healthCheckColumns']) {
      return;
    }

    info3[NX.I18n.get('BROWSE_SEARCH_VERSIONS_POPULAR')] = me.renderMostPopularVersion(model);
    componentDetails.down('#info3').showInfo(info3);
  },

  /**
   * @private
   * Loads/set Health Check fields on loaded records.
   */
  loadHealthCheckFields: function(store, records) {
    var me = this,
        components = [],
        resultList = me.getHealthcheckResult();

    if (!resultList || !resultList['healthCheckColumns']) {
      return;
    }

    Ext.Array.each(records, function(model) {
      model.beginEdit();
      model.set('healthCheckLoading', true);
      model.endEdit();
      components.push({
        id: model.getId(),
        group: model.get('group'),
        name: model.get('name'),
        version: model.get('version'),
        format: model.get('format')
      });
    });

    if (resultList) {
      resultList.getView().refresh();
    }

    NX.direct.healthcheck_Search.read(components, function(response) {
      var success = Ext.isObject(response) && response.success;
      Ext.Array.each(records, function(model) {
        model.beginEdit();
        model.set('healthCheckLoading', false);
        model.set('healthCheckError', !success);
        model.endEdit();
      });
      if (success) {
        Ext.Array.each(response.data, function(entry) {
          var model = store.getById(entry.id);
          if (model) {
            model.beginEdit();
            Ext.Object.each(entry['healthCheck'], function(key, value) {
              model.set('healthCheck' + Ext.String.capitalize(key), value);
            });
            model.endEdit();
          }
        });
      }
      if (resultList) {
        resultList.getView().refresh();
      }
    });
  },

  /**
   * @private
   * Add/Remove Health Check columns based on nexus:healthcheck:read permission.
   * @param {Ext.grid.Panel} grid to add health check columns to
   */
  bindHealthCheckColumns: function(grid) {
    var me = this;
    grid.mon(
        NX.Conditions.and(
            NX.Conditions.isPermitted('nexus:healthcheck:read')
        ),
        {
          satisfied: Ext.pass(me.addHealthCheckColumns, grid),
          unsatisfied: Ext.pass(me.removeHealthCheckColumns, grid),
          scope: me
        }
    );
  },

  /**
   * @private
   * Add Health Check columns to grid.
   * @param {Ext.grid.Panel} grid to add health check columns to
   */
  addHealthCheckColumns: function(grid) {
    var me = this,
        view = grid.getView();

    if (!grid['healthCheckColumns']) {
      grid['healthCheckColumns'] = [
        Ext.create('Ext.grid.column.Column', {
          header: NX.I18n.get('BROWSE_SEARCH_VERSIONS_AGE_COLUMN'),
          dataIndex: 'healthCheckAge',
          groupable: false,
          sortable: false,
          width: 60,
          renderer: Ext.bind(me.renderAgeColumn, me)
        }),
        Ext.create('Ext.grid.column.Column', {
          header: NX.I18n.get('BROWSE_SEARCH_VERSIONS_POPULARITY_COLUMN'),
          dataIndex: 'healthCheckPopularity',
          groupable: false,
          sortable: false,
          width: 90,
          renderer: Ext.bind(me.renderPopularityColumn, me)
        })
      ];
      grid.headerCt.add(grid.healthCheckColumns);
      grid.fireEvent('healthcheckcolumnsadded', grid);
      view.refresh();
    }
  },

  /**
   * @private
   * Remove Health Check columns from grid.
   * @param {Ext.grid.Panel} grid to remove health check columns from
   */
  removeHealthCheckColumns: function(grid) {
    if (grid['healthCheckColumns']) {
      grid.headerCt.remove(grid['healthCheckColumns']);
      grid.getView().refresh();
      delete grid['healthCheckColumns'];
    }
  },

  /**
   * @private
   * Render most popular version field.
   * @param {NX.coreui.model.Component} componentModel component model
   * @returns {string} rendered value
   */
  renderMostPopularVersion: function(componentModel) {
    var me = this,
        result, metadata = {};

    result = me.renderPreconditions(componentModel, metadata);
    if (!result) {
      result = componentModel.get('healthCheckMostPopularVersion');
      if (!result) {
        result = me.renderNotAvailable(metadata);
      }
    }
    return '<div ' + (metadata.tdAttr || '') + '>' + result + '</div>';
  },

  /**
   * @private
   * Render age column.
   * @param {number} value age
   * @param metadata column metadata
   * @param {NX.coreui.model.Component} model component model
   * @returns {string} rendered value
   */
  renderAgeColumn: function(value, metadata, model) {
    var me = this,
        result, age, dayAge;

    result = me.renderPreconditions(model, metadata);
    if (!result) {
      age = model.get('healthCheckAge');
      if (age === 0 || age > 0) {
        // convert millis to a day count
        dayAge = age / (1000 * 60 * 60 * 24);

        if (dayAge > 364) {
          result = (dayAge / 365).toFixed(1) + ' yrs';
        }
        else {
          result = dayAge.toFixed(0) + ' d';
        }
      }
      if (!result) {
        result = me.renderNotAvailable(metadata);
      }
    }
    return result;
  },

  /**
   * @private
   * Render popularity column.
   * @param {number} value popularity
   * @param metadata column metadata
   * @param {NX.coreui.model.Component} model component model
   * @returns {string} rendered value
   */
  renderPopularityColumn: function(value, metadata, model) {
    var me = this,
        result, popularity;

    result = me.renderPreconditions(model, metadata);
    if (!result) {
      popularity = model.get('healthCheckPopularity');
      if (popularity === 0 || popularity > 0) {
        if (popularity > 100) {
          popularity = 100;
        }
        result = '<img src="' + me.imageUrl('bar_start.gif')
            + '" style="vertical-align: middle;"/><img src="' + me.imageUrl('blue_body.gif')
            + '" width="' + popularity + '%"'
            + ' height="8px" style="vertical-align: middle;"/><img src="' + me.imageUrl('blue_border.gif')
            + '" style="vertical-align: middle;"/>';
      }
      if (!result) {
        result = me.renderNotAvailable(metadata);
      }
    }
    return result;
  },

  /**
   * @private
   * Render value based on preconditions.
   * @param {NX.coreui.model.Component} model component model
   * @param metadata column metadata
   * @returns {*} rendered value
   */
  renderPreconditions: function(model, metadata) {
    var me = this;

    // FIXME: resolve use of undefined "opaqueWarning" css class and potentially replace icons with glyphs

    if (model.get('healthCheckLoading')) {
      return 'Loading...';
    }
    else if (model.get('healthCheckDisabled')) {
      metadata.tdAttr = 'data-qtip="' + NX.I18n.get('BROWSE_SEARCH_VERSIONS_HEALTH_CHECK_DISABLED') + '"';
      return '<img class="opaqueWarning" src="' + me.imageUrl('information.png') + '">';
    }
    else if (model.get('healthCheckError')) {
      metadata.tdAttr = 'data-qtip="' + NX.I18n.get('BROWSE_SEARCH_VERSIONS_HEALTH_CHECK_ERROR') + '"';
      return '<img class="opaqueWarning" src="' + me.imageUrl('exclamation.gif') + '">';
    }
    else if (model.get('healthCheckCapped') || (model && model.get('capped'))) {
      metadata.tdAttr = 'data-qtip="' + NX.I18n.get('BROWSE_SEARCH_VERSIONS_HEALTH_CHECK_QUOTA_REACHED') + '"';
      return '<img class="opaqueWarning" src="' + me.imageUrl('warning.gif') + '">';
    }
    return undefined;
  },

  /**
   * @private
   * Render a not available value (no data).
   * @param metadata column metadata
   * @returns {string} rendered value
   */
  renderNotAvailable: function(metadata) {
    metadata.tdAttr = 'data-qtip="' + NX.I18n.get('BROWSE_SEARCH_VERSIONS_HEALTH_CHECK_NOT_AVAILABLE') + '"';
    return '<span class="fa fa-ban"/>';
  },

  /**
   * @private
   * Calculate image url.
   * @param name of image
   * @returns {string} image url
   */
  imageUrl: function(name) {
    return NX.util.Url.urlOf('static/rapture/resources/images/' + name);
  }

});
