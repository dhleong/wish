
var gapi = {
    auth: {},
    auth2: {},
    client: {
        drive: {
            files: {}
        }
    }
};

gapi.load = function() {};
gapi.auth.authorize = function() {};

/** @return {gapi.AuthInstance} */
gapi.auth2.getAuthInstance = function() {};
gapi.client.init = function() {};
gapi.client.drive.files.get = function() {};
gapi.client.drive.files.delete = function() {};
gapi.client.drive.files.list = function() {};
gapi.client.drive.files.update = function() {};

/** @return {gapi.ClientRequest} */
gapi.client.request = function() {};

/** @constructor */
gapi.AuthInstance = function() {};
gapi.AuthInstance.prototype.isSignedIn = {
    get: function() {},
    listen: function() {},
};
gapi.AuthInstance.prototype.signIn = function() {};
gapi.AuthInstance.prototype.signOut = function() {};

gapi.AuthInstance.prototype.currentUser = {
    /** @return {gapi.auth2.GoogleUser} */
    get: function() {},
};

/** @constructor */
gapi.ClientRequest = function() {};
gapi.ClientRequest.prototype.execute = function() {};

/** @constructor */
gapi.auth2.GoogleUser = function() {};

/** @return {gapi.auth2.AuthResponse} */
gapi.auth2.GoogleUser.prototype.getAuthResponse = function() {};

/** @constructor */
gapi.auth2.AuthResponse = function() {};

/** @type {string} */
gapi.auth2.AuthResponse.access_token;


var google = {
    picker: {
        ViewId: {
            DOCS: "all",
        },
    },
};

/** @constructor */
google.picker.View = function() {};
google.picker.View.prototype.setMimeTypes = function() {};

/** @constructor */
google.picker.DocsUploadView = function() {};

/** @constructor */
google.picker.PickerBuilder = function() {};

/** @return {google.picker.PickerBuilder} */
google.picker.PickerBuilder.prototype.addView = function() {};
/** @return {google.picker.PickerBuilder} */
google.picker.PickerBuilder.prototype.setAppId = function() {};
/** @return {google.picker.PickerBuilder} */
google.picker.PickerBuilder.prototype.setOAuthToken = function() {};
/** @return {google.picker.PickerBuilder} */
google.picker.PickerBuilder.prototype.setCallback = function() {};

/** @return {google.picker.Picker} */
google.picker.PickerBuilder.prototype.build = function() {};

/** @constructor */
google.picker.Picker = function() {};
google.picker.setVisible = function() {};
