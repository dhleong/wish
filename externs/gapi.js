
var gapi = {
    auth: {},
    auth2: {},
    client: {
        drive: {
            files: {},
        },
    },
    drive: {
        share: {},
    },
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
gapi.auth2.BasicProfile = function() {};
gapi.auth2.BasicProfile.prototype.getName = function() {};
gapi.auth2.BasicProfile.prototype.getEmail = function() {};

/** @constructor */
gapi.auth2.GoogleUser = function() {};

gapi.auth2.GoogleUser.prototype.grant = function() {};

/** @return {gapi.auth2.AuthResponse} */
gapi.auth2.GoogleUser.prototype.getAuthResponse = function() {};

/** @return {gapi.auth2.BasicProfile} */
gapi.auth2.GoogleUser.prototype.getBasicProfile = function() {};

/** @return {boolean} */
gapi.auth2.GoogleUser.prototype.hasGrantedScopes = function() {};

/** @constructor */
gapi.auth2.AuthResponse = function() {};

/** @type {string} */
gapi.auth2.AuthResponse.access_token;


/** @constructor */
gapi.drive.share.ShareClient = function() {};
gapi.drive.share.ShareClient.prototype.setOAuthToken = function() {};
gapi.drive.share.ShareClient.prototype.setItemIds = function() {};
gapi.drive.share.ShareClient.prototype.showSettingsDialog = function() {};


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
