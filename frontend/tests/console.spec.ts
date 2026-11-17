import {expect, test} from '@playwright/test';
import fs from 'node:fs';

const fixture = JSON.parse(fs.readFileSync('/tmp/log-browser-fixture.json', 'utf8'));
test('authenticated explorer, escaped logs, filters, saved queries, analytics and live tail', async ({page}) => {
    await page.goto('/');
    await page.getByText('Authenticate with an OIDC access token').click();
    await page.getByLabel('OIDC access token').fill(fixture.token);
    await page.getByRole('button', {name: 'Establish session'}).click();
    await expect(page.getByRole('status')).toHaveText('Session established');
    await page.getByLabel('Search message').fill(fixture.marker);
    await page.getByRole('button', {name: 'Run query'}).click();
    await expect(page.getByRole('status')).toHaveText('1 matching events');
    await expect(page.locator('article').first()).toContainText('<script>window.pwned=1</script>');
    expect(await page.evaluate(() => Reflect.get(window, 'pwned'))).toBeUndefined();
    await page.getByText('Event details and trace context').click();
    await expect(page.locator('article')).toContainText('test-service');
    await page.getByRole('button', {name: 'Save search', exact: true}).click();
    await expect(page.locator('.panel')).toContainText('id');
    await page.getByRole('button', {name: 'Saved searches', exact: true}).click();
    await expect(page.locator('.panel')).toContainText(fixture.marker);
    await page.getByRole('button', {name: 'Load ' + fixture.marker, exact: true}).last().click();
    await expect(page.getByRole('status')).toContainText('Saved filters loaded');
    await page.getByRole('button', {name: 'Run query'}).click();
    await expect(page.getByRole('status')).toHaveText('1 matching events');
    await page.getByRole('button', {name: 'Analytics', exact: true}).click();
    await page.getByRole('button', {name: 'Run query'}).click();
    await expect(page.locator('.panel')).toContainText('timeline');
    await page.getByRole('button', {name: 'Sources', exact: true}).click();
    await expect(page.locator('.panel')).toContainText('test-service');
    await page.getByRole('button', {name: 'Explorer', exact: true}).click();
    await page.getByRole('button', {name: 'Start / stop live tail'}).click();
    await expect(page.getByRole('status')).toContainText('Live tail connected');
    await page.getByRole('button', {name: 'Start / stop live tail'}).click();
    await expect(page.getByRole('status')).toHaveText('Live tail stopped');
});
test('unauthenticated and cross-origin operations fail', async ({request}) => {
    expect((await request.post('/api/logs/search', {data: {}})).status()).toBe(401);
    expect((await request.post('/api/session', {
        headers: {Origin: 'https://untrusted.invalid'},
        data: {token: fixture.token}
    })).status()).toBe(403);
});
