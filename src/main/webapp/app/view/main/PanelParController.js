Ext.define('BillWebApp.view.main.PanelParController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.panelParController',

    // Формирование платежки
    onLogoutClick: function () {
        console.log('Выход!');
        window.location.assign('/logout');
    }


});
