/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{vue,js,ts,jsx,tsx}'],
  theme: {
    extend: {
      colors: {
        paper: '#F4F2EE',
        surface: '#FFFFFF',
        ink: '#1C1C1E',
        muted: '#6B6B76',
        line: '#E4E1DA',
        accent: '#3B5BDB',
        favorite: '#E8A838',
        success: '#2F9E44',
        danger: '#E03131',
      },
      fontFamily: {
        sans: [
          'Inter',
          'PingFang SC',
          'Microsoft YaHei',
          'system-ui',
          '-apple-system',
          'sans-serif',
        ],
      },
      boxShadow: {
        card: '0 1px 2px rgba(28, 28, 30, 0.04), 0 1px 3px rgba(28, 28, 30, 0.03)',
        'card-hover':
          '0 2px 6px rgba(28, 28, 30, 0.06), 0 4px 12px rgba(28, 28, 30, 0.04)',
      },
      borderRadius: {
        card: '12px',
      },
      fontSize: {
        caption: ['12px', { lineHeight: '1.4' }],
      },
    },
  },
  plugins: [],
}
