// Контейнер для формы настройки платежных поручений
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
    frame: false,
    items: [{
        xtype: 'panel3'
    }]

});

