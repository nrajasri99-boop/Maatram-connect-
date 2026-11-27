/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,ts,jsx,tsx}'],
  theme: {
    extend: {
      colors: {
        maatram: {
          blue: '#1565C0',
          blueDark: '#0D47A1',
          blueSoft: '#EEF4FF',
          blueContainer: '#DCEBFF',
          navy: '#0F2544',
          outline: '#D8E3F2',
          green: '#15803D',
          greenSoft: '#ECFDF3',
          orange: '#C2410C',
          orangeSoft: '#FFF7ED',
          red: '#B91C1C',
          redSoft: '#FEF2F2',
          surface: '#F8FAFF',
        },
      },
    },
  },
  plugins: [],
};
