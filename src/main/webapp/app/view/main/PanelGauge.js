// Контейнер для формы настройки платежных поручений
Ext.define('BillWebApp.view.main.PanelGauge', {
    extend: 'Ext.panel.Panel',
    xtype: 'panelgauge',
    layout: {
        type: 'hbox',
        align: 'center',
        pack: 'center'
    },
    items:[
        {
            xtype: 'label',
            html: 'Загрузка...'
        }
    ]

});

