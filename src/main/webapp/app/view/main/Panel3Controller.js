/**
 * This class is the controller for the main view for the application. It is specified as
 * the "controller" of the Main view class.
 *
 * TODO - Replace this content of this view to suite the needs of your application.
 */
var askObjPanel;

Ext.define('BillWebApp.view.main.Panel3Controller', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.panel3controller',
    listen : {
        controller : {
            '*' : {
                something : 'onSetPayordCmpKlskFk'
            }
        }
    },

    // Событие из контроллера AskObjPanelController, установить klsk объекта
    onSetPayordCmpKlskFk : function(controller, id, name) {

        var payordCmpGrid = this.lookupReference('payordCmpGrid');
        var store = payordCmpGrid.getStore();

        var row = payordCmpGrid.getSelectionModel().getSelection()[0];
        var rec = store.findRecord('id', row.get('id'));

        rec.set('koFk', id);
        rec.set('koName', name);

        var panel3 = this.getView();
        panel3.focus();
    },

    onGridPayordGrpEdit: function() {
        var store = this.getViewModel().getStore('payordgrpstore');
        store.load;
        var view = this.getView();
        view.refresh();
    },

    // Добавить группу платежек
    onGridPayordGrpAdd: function() {
        var rec = new BillWebApp.model.PayordGrp({
            name: 'Заполнить наименование!'
        });
        var store = this.getViewModel().getStore('payordgrpstore');
        store.insert(0, rec);
        var payordGrpGrid = this.lookupReference('payordGrpGrid');
        payordGrpGrid.findPlugin('cellediting').startEdit(rec, 1);
    },

    // Добавить платежку в группу
    onGridPayordAdd: function() {
        var payordstore = this.getViewModel().getStore('payordstore');
        var payordGrpGrid = this.lookupReference('payordGrpGrid');
        var payordGrid = this.lookupReference('payordGrid');
        var rec = new BillWebApp.model.Payord({
            name: 'Заполнить наименование!',
            payordGrpFk : payordGrpGrid.selection.id
        });
        payordstore.insert(0, rec);
        payordGrid.findPlugin('cellediting').startEdit(rec, 1);
    },

    // Добавить формулу в платежку
    onGridPayordCmpAdd: function() {
        var payordcmpstore = this.getViewModel().getStore('payordcmpstore');
        var payordCmpGrid = this.lookupReference('payordCmpGrid');
        var payordGrid = this.lookupReference('payordGrid');
        var rec = new BillWebApp.model.PayordCmp({
            payordFk : payordGrid.selection.id
        });
        payordcmpstore.insert(0, rec);
        payordCmpGrid.findPlugin('cellediting').startEdit(rec, 1);
    },

    // Отменить отредактированную группу платежек
    onGridPayordGrpCancel: function() {
        var store = this.getViewModel().getStore('payordgrpstore');
        store.rejectChanges();
        //store.load(); // временно убрал
    },

    // Отменить отредактированную платежку
    onGridPayordCancel: function() {
        var payordGrpGrid = this.lookupReference('payordGrpGrid');
        var store = this.getViewModel().getStore('payordstore');
        store.rejectChanges();
        /*store.load({// временно убрал
            params : {
                'payordGrpId': payordGrpGrid.selection.id
            }
        });*/
    },

    // Отменить отредактированную формулу платежки
    onGridPayordCmpCancel: function() {
        var payordGrid = this.lookupReference('payordGrid');
        var store = this.getViewModel().getStore('payordcmpstore');
        store.rejectChanges();
        /*store.load({ // временно убрал
            params: {
                'payordId': payordGrid.selection.id
            }
        });*/
    },

    // Сохранить отредактированную группу платежек
    onGridPayordGrpUpd: function() {
        var store = this.getViewModel().getStore('payordgrpstore');
        store.sync({
            failure: function (batch, options) {
                alert("Ошибка сохранения группы платежек, возможно не все поля заполнены!");
            }
        });
    },

    // Сохранить отредактированную платежку
    onGridPayordUpd: function() {
        var store = this.getViewModel().getStore('payordstore');
        store.sync({
            failure: function (batch, options) {
                alert("Ошибка сохранения платежки, возможно не все поля заполнены!");
            }
        });
    },

    // Сохранить отредактированную формулу платежки
    onGridPayordCmpUpd: function() {
        //validCheckSync(this, 'payordcmpstore');
        var store = this.getViewModel().getStore('payordcmpstore');
        store.sync({
            failure: function (batch, options) {
                alert("Ошибка сохранения формулы платежки, возможно не все поля заполнены!");
            }
        });
    },

    // Удалить платежку
    onGridPayordDel: function(grid, rowIndex, colIndex)  {
        Ext.create('Ext.window.MessageBox', {
            multiline: false,
        }).show({
            title: 'Удаление записи',
            msg: 'Вы уверены, удалить платежку?',
            closable: false,
            buttons: Ext.Msg.YESNO,
            buttonText: {
                yes: 'Да',
                no: 'Нет'
            },
            fn: function (buttonValue, inputText, showConfig) {
                if (buttonValue == 'yes') {
                    var rec = grid.getStore().getAt(rowIndex);
                    var store = grid.getStore();
                    store.remove(rec);
                    store.sync();
                }
            },
            icon: Ext.Msg.QUESTION
        });
    },

    // Удалить формулу платежки
    onGridPayordCmpDel: function(grid, rowIndex, colIndex)  {
        Ext.create('Ext.window.MessageBox', {
            multiline: false,
        }).show({
            title: 'Удаление записи',
            msg: 'Вы уверены, удалить формулу платежки?',
            closable: false,
            buttons: Ext.Msg.YESNO,
            buttonText: {
                yes: 'Да',
                no: 'Нет'
            },
            fn: function (buttonValue, inputText, showConfig) {
                if (buttonValue == 'yes') {
                    var rec = grid.getStore().getAt(rowIndex);
                    var store = grid.getStore();
                    store.remove(rec);
                    store.sync();
                }
            },
            icon: Ext.Msg.QUESTION
        });
    },

    // Удалить группу платежек
    onGridPayordGrpDel: function(grid, rowIndex, colIndex)  {
        Ext.create('Ext.window.MessageBox', {
            multiline: false,
        }).show({
            title: 'Удаление записи',
            msg: 'Вы уверены, удалить группу платежек?',
            closable: false,
            buttons: Ext.Msg.YESNO,
            buttonText: {
                yes: 'Да',
                no: 'Нет'
            },
            fn: function (buttonValue, inputText, showConfig) {
                if (buttonValue == 'yes') {
                    var rec = grid.getStore().getAt(rowIndex);
                    var store = grid.getStore();
                    store.remove(rec);
                    store.sync();
                }
            },
            icon: Ext.Msg.QUESTION
        });
    },

    onGridPayordRowClick: function(grid, rec) {
        var store = this.getViewModel().getStore('payordcmpstore');
        var payordCmpGrid = this.lookupReference('payordCmpGrid');
        store.load({
            params : {
                'payordId': rec.get('id')
            },
            callback: function(records, operation, success) {
                if (records.length > 0) {
                    payordCmpGrid.getSelectionModel().select(0);
                }
            }
        });
    },

    onGridPayordGrpRowClick: function(grid, rec) {

        var store = this.getViewModel().getStore('payordstore');
        var store1 = this.getViewModel().getStore('payordcmpstore');
        var payordGrid = this.lookupReference('payordGrid');
        var payordCmpGrid = this.lookupReference('payordCmpGrid');
        store.load({
            params : {
                'payordGrpId': rec.get('id')
            },
            callback: function(records, operation, success) {
                if (success) {
                    var rec1 = records[0];
                    var id = -1;
                    if (records.length > 0) {
                        id = records[0].get('id')
                        payordGrid.getSelectionModel().select(0); // если включить и будет 0 записей - вызовет model is null
                    } else {
                        id = -1;
                    }
                    // загрузить формулы платежки
                    store1.load({
                        params: {
                            'payordId': id
                        },
                        callback: function(records, operation, success) {
                            if (records.length > 0) {
                                payordCmpGrid.getSelectionModel().select(0); // если включить и будет 0 записей - вызовет model is null
                            }
                        }
                    });

                } else {
                    console.log('onGridPayordGrpRowClick: NOT SUCCESS');
                }
            }

        });
    },

    // выбор периодичности платежки
    onGridPayordPeriodRender: function (value, metaData, record, rowIndex, colIndex, store, view) {
        var store = this.getViewModel().getStore('lststore');
        var index = store.findExact('id', value);
        if (index != -1){
            var rs = store.getAt(index);
            return rs.get('name');
        }
    },

    // выбор варианта сбора формулы платежки
    onGridPayordCmpVarRender: function (value, metaData, record, rowIndex, colIndex, store, view) {
        var store = this.getViewModel().getStore('varstore');
        var index = store.findExact('id', value);
        if (index != -1){
            var rs = store.getAt(index);
            return rs.get('name');
        }
    },

    // выбор услуги формулы платежки
    onGridPayordCmpServRender: function (value, metaData, record, rowIndex, colIndex, store, view) {
        var store = this.getViewModel().getStore('servstore');
        var index = store.findExact('id', value);
        if (index != -1){
            var rs = store.getAt(index);
            return rs.get('name');
        }
    },

    // выбор организации формулы платежки
    onGridPayordCmpOrgRender: function (value, metaData, record, rowIndex, colIndex, store, view) {
        var store = this.getViewModel().getStore('orgstore');
        var index = store.findExact('id', value);
        if (index != -1){
            var rs = store.getAt(index);
            return rs.get('name');
        } else {
            return '';
        }
    },

    // рендерер поля mark
    onGridPayordCmpMarkRender: function (value, metaData, record, rowIndex, colIndex, store) {
       validRenderer(value, metaData, record, rowIndex, colIndex, store);
       return value;
    },

    // выбор объекта формулы платежки
    onGridPayordCmpAreaRender: function (value, metaData, record, rowIndex, colIndex, store, view) {
        var store = this.getViewModel().getStore('areastore');
        var index = store.findExact('id', value);
        if (index != -1){
            var rs = store.getAt(index);
            return rs.get('name');
        }
    },

    // Открыть панель выбора объекта
    onGridPayordCmpItemSel: function() {

        askObjPanel = new Ext.form.Panel({
            modal: true,
            title: 'Поиск объекта',
            floating: true,
            closable : true,
            resizable : true,
            layout: 'fit',

            //viewModel: { //- надо или нет?
            //    type: 'main'
            //},
            //controller: 'main',

            items: [
                { xtype: 'askobjpanel' }
            ]
        });
        askObjPanel.show();

        /*var ppp = this.lookupReference('payordGrpGrid'); // убрать
        console.log('Grid='+ppp);
        var vm = this.getViewModel();
        console.log('View.id='+vm.id);
        console.log('View.type='+vm.type);*/

    }
});
