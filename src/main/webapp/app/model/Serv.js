/**
 * Created by lev on 15.03.2017.
 */
Ext.define('BillWebApp.model.Serv', {
    extend: 'Ext.data.Model',
    fields: [
        { name: 'id', type: 'int', convert: null },
        { name: 'cd', type: 'string' },
        { name: 'name', type: 'string' }
    ]
});