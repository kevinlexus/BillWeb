/**
 * This class is the controller for the main view for the application. It is specified as
 * the "controller" of the Main view class.
 *
 * TODO - Replace this content of this view to suite the needs of your application.
 */
Ext.define('BillWebApp.view.main.Panel2Controller', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.panel2controller',

    // Сохранить отредактированное платежное поручение
    onGridPayordFlowUpd: function() {
        console.log('test');
        var store = this.getViewModel().getStore('payordflowstore');
        store.sync({
            failure: function (batch, options) {
                alert("Ошибка сохранения платежки, возможно не все поля заполнены!");
            }
        });
    },
    // Добавить движение по платежке (платежное поручение)
    onGridPayordFlowAdd: function() {
        /*var payordstore = this.getViewModel().getStore('payordstore');
        var payordGrpGrid = this.lookupReference('payordGrpGrid');
        var payordGrid = this.lookupReference('payordGrid');
        var rec = new BillWebApp.model.Payord({
            name: 'Заполнить наименование!',
            payordGrpFk : payordGrpGrid.selection.id
        });*/
        var rec = new BillWebApp.model.Payord({
            summa: 1111,
            tp: 2,
            period: '201704'
        });
        var payordFlowGrid = this.lookupReference('payordFlowGrid');
        var payordflowstore = this.getViewModel().getStore('payordflowstore');
        payordflowstore.insert(0, rec);
        payordFlowGrid.findPlugin('cellediting').startEdit(rec, 1);
    },

    // Удалить движение по платежке
    onGridPayordFlowDel: function(grid, rowIndex, colIndex)  {
        Ext.create('Ext.window.MessageBox', {
            multiline: false,
        }).show({
            title: 'Удаление записи',
            msg: 'Вы уверены, удалить платежное поручение?',
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
    // Отменить отредактированное платежное поручение
    onGridPayordFlowCancel: function() {
        var store = this.getViewModel().getStore('payordflowstore');
        store.rejectChanges();
    },

    // выбор платежки
    onGridPayordFlowPayordRender: function (value, metaData, record, rowIndex, colIndex, store, view) {
        var store = this.getViewModel().getStore('payordstore');
        var index = store.findExact('id', value);
        if (index != -1){
            var rs = store.getAt(index);
            return rs.get('name');
        }
    },
    // выбор УК
    onGridPayordFlowUkRender: function (value, metaData, record, rowIndex, colIndex, store, view) {
        var store = this.getViewModel().getStore('orgstore');
        var index = store.findExact('id', value);
        if (index != -1){
            var rs = store.getAt(index);
            return rs.get('name');
        } else {
            console.log('Возможно не загружен OrgStore');
        }
    },
    // Обновить грид платежных поручений
    onGridPayordFlowRefresh: function () {
        var store = this.getViewModel().getStore('payordflowstore');
        var genDt2 = this.lookupReference('genDt2');

        store.load({
            params : {
                dt: Ext.Date.format(genDt2.getValue(), 'd.m.Y')
            }
        });
    },
    // Подписать все платежки
    onGridPayordFlowSignAll: function () {
        console.log("onGridPayordFlowSignAll");
        var store = this.getViewModel().getStore('payordflowstore');
        var genDt2 = this.lookupReference('genDt2');
        store.each(function(record){
            record.set('signed', true);
        });
        store.sync({
            failure: function (batch, options) {
                alert("Ошибка подписи платежек!");
            },
            success: function() {
                store.load({
                    params : {
                        dt: Ext.Date.format(genDt2.getValue(), 'd.m.Y')
                    }
                });
            }
        });
    },
    // Снять подпись со всех платежек
    onGridPayordFlowUnSignAll: function () {
        console.log("onGridPayordFlowSignAll");
        var store = this.getViewModel().getStore('payordflowstore');
        var genDt2 = this.lookupReference('genDt2');
        store.each(function(record){
            record.set('signed', false);
        });
        store.sync({
            failure: function (batch, options) {
                alert("Ошибка снятия подписи платежек!");
            },
            success: function() {
                store.load({
                    params : {
                        dt: Ext.Date.format(genDt2.getValue(), 'd.m.Y')
                    }
                });
            }
        });
    }


    });
