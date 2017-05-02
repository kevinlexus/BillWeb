/**
 * This class is the view model for the Main view of the application.
 */
Ext.define('BillWebApp.view.main.MainModel', {
    extend: 'Ext.app.ViewModel',

    alias: 'viewmodel.main',

    data: {
        name: 'BillWebApp',
        loremIpsum: 'Редактирование платежек',
        periodId1: '',
        periodId2: '',
        orgId: '',
        tp: 'PAYORD_PERIODTP'
    },

    stores: {
        periodstore1: {
            type: 'periodstore1',
            proxy : {
                extraParams: {
                    repCd: 'RptPayDocList',
                    tp: '0'
                }
            }
        },
        periodstore2: {
            type: 'periodstore1',
            proxy : {
                extraParams: {
                    repCd: 'RptPayDocList',
                    tp: '0'
                }
            }
        },
        payordstore: {
            type: 'payordstore',
            proxy : {
                extraParams : {
                    payordGrpId : 0 // по умолчанию - все платежки
                }
            }
        },
        payordgrpstore: {
            type: 'payordgrpstore'
        },
        payordcmpstore: {
            type: 'payordcmpstore'
        },
        payordflowstore: {
            type: 'payordflowstore'
        },
        lststore: {
            type: 'lststore',
            proxy : {
                extraParams : {
                    tp : 'PAYORD_PERIODTP'
                }
            }
        },
        areastore: {
            type: 'lststore',
            proxy : {
                extraParams : {
                    tp : 'AREA_TP'
                }
            }
        },
        varstore: {
            type: 'lststore',
            proxy : {
                extraParams : {
                    tp : 'PAYORD_SRC_TP'
                }
            }
        },
        addrMainTpStore: { //список типов адресов ограниченный основными типами!
            type: 'addrtpstore',
            proxy : {
                extraParams : {
                    tp : 1
                }
            }
        },
        koAddrTpStore: {
            type: 'kostore'
        },
        servstore: {
            type: 'servstore'
        },
        orgstore: {
            type: 'orgstore'
        },
        orgcuruserstore: {
            type: 'orgcuruserstore'
        }
    }

});
