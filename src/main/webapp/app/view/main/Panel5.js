Ext.define('BillWebApp.view.main.Panel5', {
    extend: 'Ext.panel.Panel',
    xtype: 'panel5',
    layout: {
        type: 'vbox'
    },
    scrollable: true,
    maxHeight: 900,
    bodyPadding: 10,
    reference: 'panel5',
    controller: 'panel4controller',
    frame: false,
    items: [{
        xtype: 'panel3'
    }]

});

