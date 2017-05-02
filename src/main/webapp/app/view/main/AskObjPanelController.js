/**
 * This class is the controller for the main view for the application. It is specified as
 * the "controller" of the Main view class.
 *
 * TODO - Replace this content of this view to suite the needs of your application.
 */
Ext.define('BillWebApp.view.main.AskObjPanelController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.askobjpanelcontroller',

    // В AskObjPanel: Обновить грид, при нажатии кнопки поиска
    onAskObjButtonFindPress: function() {
        var askObjComboAdrTp = this.lookupReference('askObjComboAdrTp');
        var koAddrTpStore = this.getViewModel().getStore('koAddrTpStore');
        var askObjName = this.lookupReference('askObjName');
        koAddrTpStore.load({params: {
            'addrTp': askObjComboAdrTp.getValue(),
            'flt': askObjName.getValue()
        }});
    },

    // В AskObjPanel: Записать новое значение в поле грида, при нажатии кнопки выбора
    onAskObjButtonSelPress: function() {
        var koAddrTpStore = this.getViewModel().getStore('koAddrTpStore');
        var askObjGrid = this.lookupReference('askObjGrid');
        // получить текущую выделенную запись
        var row = askObjGrid.getSelectionModel().getSelection()[0];
//        console.log('Ask:Name='+ row.get('id')+' '+row.get('name'));
        // вызвать событие в контроллере панели Panel3 // TODO Разобраться как это вызывается!
        this.fireEvent('something', this, row.get('id'), row.get('name'));
        askObjPanel.destroy();
    }
});
