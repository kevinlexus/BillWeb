Ext.define('BillWebApp.view.main.PanelPar', {
    extend: 'Ext.form.Panel',
    xtype: 'panelPar',
    title: 'Параметры',
    frame: true,
    resizable: true,
    width: 610,
    minWidth: 610,
    minHeight: 300,

    defaults: {
        layout: 'form',
        xtype: 'container',
        style: 'width: 50%'
    },
    reference: 'panelPar',
    controller: 'panelParController',
    items: [],
    buttons: [{
        text: 'Выход из приложения',
        handler: 'onLogoutClick'
    }]
});

