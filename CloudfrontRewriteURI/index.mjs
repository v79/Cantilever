'use strict';
export const handler = async (event, context, callback) => {

	let request = event.Records[0].cf.request;
	const response = event.Records[0].cf.response;
	const headers = request.headers;
	const hostHeader = headers['host'][0].value;
	const rqUri = request.uri;

	let newUri;

	// Define routing logic based on domain name
	console.log('Original request uri: ' + rqUri);
	console.log('Original hostHeader: ' + hostHeader);
	newUri = '/' + hostHeader + rqUri;
	// Match any '/' that occurs at the end of a URI. Replace it with a default index
	newUri = newUri.replace(/\/$/, '\/index.html');

	// Update the request URI
	request.uri = newUri;
	console.log('newUri: ' + newUri);
	request.headers = headers;

	callback(null, request);
};

