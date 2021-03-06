Ext.define('BillWebApp.view.main.Panel1', {
    extend: 'Ext.form.Panel',
    xtype: 'panel1',
    title: 'Печать платежек',
    frame: true,
    resizable: true,
    width: 610,
    minWidth: 610,
    minHeight: 300,

    defaults: {
        layout: 'form',
        xtype: 'container',
        style: 'width: 100%'
    },
    controller: 'panel1controller', // Обязательно указывать контроллер, иначе не будет привязан нужный store!!!

        items: [
            {
                // блок выбора периода
                xtype: 'fieldset',
                title: 'Период',
                defaultType: 'textfield',
                layout: {
                    type: 'vbox',
                    align: 'stretch'
                },
                defaults: {
                    anchor: '100%'
                },
                items: [
                    {
                        xtype: 'combobox',
                        fieldLabel: 'С',
                        displayField: 'period',
                        valueField: 'id',
                        reference: 'period1',
                        maxWidth: 300,
                        queryMode: 'local',
                        selectOnFocus: true,
                        listeners: {
                            beforequery: function (record) {
                                record.query = new RegExp(record.query, 'i');
                                record.forceAll = true;
                            }
                        },
                        bind: {
                            store: '{periodstore1}',
                            value: '{periodId1}'
                        }

                    },
                    {
                        xtype: 'combobox',
                        fieldLabel: 'По',
                        displayField: 'period',
                        valueField: 'id',
                        reference: 'period2',
                        maxWidth: 300,
                        queryMode: 'local',
                        selectOnFocus: true,
                        listeners: {
                            beforequery: function (record) {
                                record.query = new RegExp(record.query, 'i');
                                record.forceAll = true;
                            }
                        },
                        bind: {
                            store: '{periodstore2}',
                            value: '{periodId2}'
                        }
                    }/*, {
                        xtype: 'checkboxfield',
                        name: 'checkbox1',
                        reference: 'checkbox1',
                        fieldLabel: 'Текущие',
                        listeners: {
                            change: 'onChangePeriodType'
                        }
                    }*/
                ]
            },
            {
                // блок выбора параметров
                xtype: 'fieldset',
                title: 'Параметры',
                defaultType: 'textfield',
                layout: {
                    type: 'vbox',
                    align: 'stretch'
                },
                items: [
                    {
                        xtype: 'combobox',
                        fieldLabel: 'Платежные поручения',
                        displayField: 'name',
                        valueField: 'id',
                        queryMode: 'local',
                        selectOnFocus: true,
                        listeners: {
                            beforequery: function (record) {
                                record.query = new RegExp(record.query, 'i');
                                record.forceAll = true;
                            }
                        },
                        bind: {
                            store: '{payordgrpstore}',
                            value: '{payordgrpId1}'
                        }
                    },
                    {
                        xtype: 'combobox',
                        fieldLabel: 'Ук',
                        displayField: 'name',
                        valueField: 'id',
                        reference: 'states',  //????????? что это?
                        matchFieldWidth: false,

                        queryMode: 'local',
                        selectOnFocus: true,
                        listeners: {
                            beforequery: function (record) {
                                record.query = new RegExp(record.query, 'i');
                                record.forceAll = true;
                            }
                        },

                        bind: {
                            store: '{ukstore}',
                            value: '{id}'
                        },
                    }, {
                        xtype: 'checkboxfield',
                        name: 'checkbox1',
                        boxLabel: 'Детализация по платежкам'
                    }, {
                        xtype: 'checkboxfield',
                        name: 'checkbox1',
                        boxLabel: 'Группировать'
                    }
                ]
            }
        ],

    buttons: [{
        text: 'Сверка',
        listeners: {
            click: 'onCompareClick'
        }
    },{
        text: 'Отчет',
        listeners: {
            click: 'onPrintClick'
        }
    }]
});