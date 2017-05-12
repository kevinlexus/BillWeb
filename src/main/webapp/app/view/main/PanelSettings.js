Ext.define('BillWebApp.view.main.PanelEdit', {
    extend: 'Ext.panel.Panel',
    xtype: 'panelEdit',
    layout: {
        type: 'vbox'
    },
    scrollable: true,
    maxHeight: 900,
    bodyPadding: 10,
    reference: 'panelEdit',
    frame: false,
    items: [{
        xtype: 'panel2'
    }]

});

