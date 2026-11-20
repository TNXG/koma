import { Want } from '@kit.AbilityKit';
import { abilityDelegatorRegistry, Driver, ON } from '@kit.TestKit';
import { beforeEach, describe, expect, it } from '@ohos/hypium';

export default function abilityTest() {
  describe('ActsAbilityTest', () => {
    const driver = Driver.create();
    const delegator = abilityDelegatorRegistry.getAbilityDelegator();
    const want: Want = {
      bundleName: 'moe.tnxg.koma',
      moduleName: 'entry',
      abilityName: 'EntryAbility'
    };

    // Run on a fresh, logged-out test installation with -s timeout 60000.
    // These flows neither contact a school nor change persisted user data.
    beforeEach(async () => {
      await delegator.startAbility(want);
      await driver.waitForComponent(ON.text('欢迎使用 Koma'), 10000);
    });

    it('systemBackReturnsFromSchoolsToWelcome', 0, async () => {
      await (await driver.waitForComponent(ON.text('开始使用'), 5000)).click();
      await driver.waitForComponent(ON.text('登录哪所学校？'), 5000);
      await driver.pressBack();
      const welcome = await driver.waitForComponent(ON.text('欢迎使用 Koma'), 5000);
      expect(await welcome.getText()).assertEqual('欢迎使用 Koma');
    });

    it('homeAndResumeRetainTheRouteAndOpenSheet', 0, async () => {
      await (await driver.waitForComponent(ON.text('导入离线课表'), 5000)).click();
      await (await driver.waitForComponent(ON.text('用 AI 识图导入'), 5000)).click();
      await driver.waitForComponent(ON.text('复制提示词'), 5000);
      await driver.pressHome();
      // Cross the renderer's delayed surface cleanup without destroying the ability.
      await driver.delayMs(2500);
      await delegator.startAbility(want);
      const copy = await driver.waitForComponent(ON.text('复制提示词'), 5000);
      expect(await copy.getText()).assertEqual('复制提示词');
      await driver.pressBack();
      await driver.waitForComponent(ON.text('预览课表'), 5000);
      await driver.pressBack();
      const welcome = await driver.waitForComponent(ON.text('欢迎使用 Koma'), 5000);
      expect(await welcome.getText()).assertEqual('欢迎使用 Koma');
    });
  });
}