import {defineConfig} from '@playwright/test';

export default defineConfig({
    testDir: './tests',
    workers: 1,
    use: {baseURL: 'http://127.0.0.1:3100', headless: true, launchOptions: {executablePath: process.env.CHROMIUM_PATH}},
    reporter: [['list'], ['json', {outputFile: '../docs/results/browser.json'}]]
});
