Ext.define('BillWebApp.view.main.PanelPar', {
    extend: 'Ext.panel.Panel',
    xtype: 'panelPar',
    title: 'Параметры',
    layout: 'form',
    width: 500,
    minHeight: 300,
    bodyPadding: 10,
    reference: 'panelPar',
    controller: 'panelParController',
    frame: true,

    buttons: [{
        text   : 'Выход из приложения',
        handler: 'onLogoutClick'
    }]

});

