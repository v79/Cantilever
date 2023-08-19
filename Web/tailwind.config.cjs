/** @type {import('tailwindcss').Config} */

const colors = require('tailwindcss/colors');

module.exports = {
  content: ['./src/**/*.{html,js,svelte,ts}',
  "./node_modules/flowbite-svelte/**/*.{html,js,svelte,ts}"],
 
  theme: {},

  plugins: [
    require('@tailwindcss/forms'),
    require('flowbite/plugin')
  ],
  darkMode: 'class',
}
