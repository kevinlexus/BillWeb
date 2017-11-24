Ext.define('BillWebApp.view.main.Panel4', {
    extend: 'Ext.panel.Panel',
    xtype: 'panel4',
    title: 'Формирование платежек',
    layout: 'form',
    width: 500,
    minHeight: 300,
    bodyPadding: 10,
    reference: 'panel4',
    controller: 'panel4controller',
    frame: true,
    items: [{
        xtype: 'datefield',
        name: 'genDt',
        reference: 'genDt',
        startDay : 1,
        fieldLabel: 'Дата формирования',
        margin: '0 5 0 0',
        allowBlank: false,
        format: 'd.m.Y',
        value: new Date()
        },
        {
            xtype: 'radiofield',
            name: 'radio1',
            value: 'isDay',
            fieldLabel: '',
            boxLabel: 'На дату (уже сформированные платежки будут удалены)'
        },
        {
            xtype: 'radiofield',
            name: 'radio1',
            reference: 'isFinalValue',
            value: 'isFinalValue',
            fieldLabel: '',
            boxLabel: 'Итоговая платежка (Обычно дата следующего месяца или 31 число, если конец года)'
        }, {
            xtype: 'radiofield',
            name: 'radio1',
            reference: 'isEndMonthValue',
            value: 'isEndMonthValue',
            fieldLabel: '',
            labelSeparator: '',
            hideEmptyLabel: false,
            boxLabel: 'Итоговое формирование (Строго после формирования и подписания итоговой платежки!)'
        }
    ],

    buttons: [{
        text   : 'Сформировать',
        handler: 'onGenClick'
    }]

});

