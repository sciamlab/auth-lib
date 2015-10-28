package com.sciamlab.auth;


import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.sciamlab.auth.model.Role;
import com.sciamlab.auth.model.User;
import com.sciamlab.auth.model.UserLocal;

public class RolesTest {

	@Before
	public final void setUp() throws Exception { }
	
	@After
	public final void tearDown() throws Exception {	}

	@Test
	public void testAnonymousRole() {
		Assert.assertNotNull(Role.ANONYMOUS);
		System.out.println(Role.ANONYMOUS);
	}
	
	@Test
	public void testCreateUser() {
		User user = new UserLocal("paolo@sciamlab.com");
		Assert.assertNotNull(user);
		System.out.println(user.toJSON().toString(2));
	}
	
	@Test
	public void testCreateRole() {
		Role admin = new Role("admin").description("Admin group");
		Assert.assertNotNull(admin);
		System.out.println(admin);
	}
	
	@Test
	public void testAddRoleToUser() {
		Role admin = new Role("admin").description("Admin group");
		User user = new UserLocal("paolo@sciamlab.com");
		user.addRole(admin);
		Assert.assertTrue(admin.roleMembers().isEmpty());
		Assert.assertFalse(admin.userMembers().isEmpty());
		System.out.println(user.toJSON().toString(2));
		Assert.assertTrue(user.hasRole(admin));
		Assert.assertTrue(admin.hasMember(user));
	}
	
	@Test
	public void testAddRoleToRole() {
		Role admin = new Role("admin").description("Admin group");
		Role superadmin = new Role("superadmin").description("SuperAdmin group");
		superadmin.addRole(admin);
		Assert.assertFalse(admin.roleMembers().isEmpty());
		System.out.println(admin.roleMembers());
		Assert.assertTrue(superadmin.hasRole(admin));
	}
	
	@Test
	public void testCheckUserIfMemberOfParentRole() {
		Role superadmin = new Role("superadmin").description("SuperAdmin group");
		Role admin = new Role("admin").description("Admin group");
		superadmin.addRole(admin);
		User user = new UserLocal("paolo@sciamlab.com");
		user.addRole(superadmin);
		System.out.println(user.toJSON().toString(2));
		Assert.assertTrue(user.hasRole(admin));
		Assert.assertTrue(admin.hasMember(user));
	}
	
	@Test
	public void testGetAllRoles() {
		Role superadmin = new Role("superadmin").description("SuperAdmin group");
		Role admin = new Role("admin").description("Admin group");
		superadmin.addRole(admin);
		User user = new UserLocal("paolo@sciamlab.com");
		user.addRole(superadmin);
		System.out.println(user.toJSON().toString(2));
		Assert.assertTrue(user.getAllRoles().contains(admin));
	}
	
	@Test
	public void testAddAlreadyAddedMemberToRole() {
		Role superadmin = new Role("superadmin").description("SuperAdmin group");
		Role admin = new Role("admin").description("Admin group");
		superadmin.addRole(admin);
		User user = new UserLocal("paolo@sciamlab.com");
		user.addRole(superadmin);
		Assert.assertTrue(admin.hasMember(user));
		Assert.assertFalse(admin.addMember(user));
	}
	
	@Test
	public void testCyclicDependencyForRoles() {
		Role admin = new Role("admin").description("Admin group");
		Role superadmin = new Role("superadmin").description("SuperAdmin group");
		superadmin.addRole(admin);
		Assert.assertFalse(admin.addMember(superadmin));
		Assert.assertTrue(admin.hasMember(superadmin));
		Exception circularexception = null;
		try {
			admin.addRole(superadmin);
		} catch (Exception e) {
			circularexception= e;
			System.out.println(circularexception.getMessage());
		}
		Assert.assertNotNull(circularexception);
		Exception circularexception2 = null;
		try {
			superadmin.addMember(admin);
		} catch (Exception e) {
			circularexception2= e;
			System.out.println(circularexception2.getMessage());
		}
		Assert.assertNotNull(circularexception2);
	}
	
	@Test
	public void testCyclicDependencyForRolesWithThreeNodes() {
		Role editor = new Role("editor").description("Editors group");
		Role admin = new Role("admin").description("Admin group");
		editor.addMember(admin);
		Assert.assertTrue(editor.hasMember(admin));
		Role superadmin = new Role("superadmin").description("SuperAdmin group");
		superadmin.addRole(admin);
		Assert.assertFalse(admin.addMember(superadmin));
		Assert.assertTrue(admin.hasMember(superadmin));
		Exception circularexception = null;
		try {
			editor.addRole(superadmin);
		} catch (Exception e) {
			circularexception= e;
			System.out.println(circularexception.getMessage());
		}
		Assert.assertNotNull(circularexception);
		Exception circularexception2 = null;
		try {
			superadmin.addMember(editor);
		} catch (Exception e) {
			circularexception2= e;
			System.out.println(circularexception2.getMessage());
		}
		Assert.assertNotNull(circularexception2);
	}
	
	@Test
	public void testACyclicDependencyWithFourNodes() {
		Role employee = new Role("employee");
		Role ps = new Role("ps");
		employee.addMember(ps);
		Role manager = new Role("manager");
		employee.addMember(manager);
		Role ps_manager = new Role("ps_manager");
		ps.addMember(ps_manager);
		manager.addMember(ps_manager);
		Assert.assertTrue(employee.hasMember(ps_manager));
		System.out.println(manager.roleMembers());
		System.out.println(ps.roleMembers());
		User jerome = new UserLocal("jerome@progress.com");
		Assert.assertTrue(jerome.addRole(employee));
		Assert.assertTrue(jerome.addRole(ps_manager));
		System.out.println(jerome.getAllRoles());
		Assert.assertFalse(jerome.addRole(manager));
	}
	
//	@Test
//	public void test() {
//		
//	}
}
