/**
 * Copyright &copy; 2012-2016 <a href="https://github.com/thinkgem/jeesite">JeeSite</a> All rights reserved.
 */
package com.thinkgem.jeesite.modules.accountant.web;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.thinkgem.jeesite.modules.sys.utils.UserUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.alibaba.druid.support.json.JSONUtils;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.thinkgem.jeesite.common.config.Global;
import com.thinkgem.jeesite.common.web.BaseController;
import com.thinkgem.jeesite.common.utils.StringUtils;
import com.thinkgem.jeesite.modules.accountant.dao.BookDao;
import com.thinkgem.jeesite.modules.accountant.dto.BookDto;
import com.thinkgem.jeesite.modules.accountant.entity.Book;
import com.thinkgem.jeesite.modules.accountant.service.BookService;

/**
 * 会计国标账本(科目)Controller
 * @author nideyuan
 * @version 2017-09-10
 */
@Controller
@RequestMapping(value = "${adminPath}/accountant/book")
public class BookController extends BaseController {

	@Autowired
	private BookService bookService;
	
	@ModelAttribute
	public Book get(@RequestParam(required=false) String id) {
		Book entity = null;
		if (StringUtils.isNotBlank(id)){
			entity = bookService.get(id);
		}
		if (entity == null){
			entity = new Book();
		}
		return entity;
	}
	
	@RequiresPermissions("accountant:book:view")
	@RequestMapping(value = {"list", ""})
	public String list(Book book, HttpServletRequest request, HttpServletResponse response, Model model) {
		List<Book> list = bookService.findList(book); 
		model.addAttribute("list", list);
		return "modules/accountant/bookList";
	}

	@RequiresPermissions("accountant:book:view")
	@RequestMapping(value = "form")
	public String form(Book book, Model model) {
		if (book.getParent()!=null && StringUtils.isNotBlank(book.getParent().getId())){
			book.setParent(bookService.get(book.getParent().getId()));
			// 获取排序号，最末节点排序号+30
			if (StringUtils.isBlank(book.getId())){
				Book bookChild = new Book();
				bookChild.setParent(new Book(book.getParent().getId()));
				List<Book> list = bookService.findList(book); 
				if (list.size() > 0){
					book.setSort(list.get(list.size()-1).getSort());
					if (book.getSort() != null){
						book.setSort(book.getSort() + 30);
					}
				}
			}
		}
		if (book.getSort() == null){
			book.setSort(30);
		}
		model.addAttribute("book", book);
		return "modules/accountant/bookForm";
	}

	@RequiresPermissions("accountant:book:edit")
	@RequestMapping(value = "save")
	public String save(Book book, Model model, RedirectAttributes redirectAttributes) {
		if (!beanValidator(model, book)){
			return form(book, model);
		}
		String parentId = book.getParentId();
		if (!StringUtils.isBlank(parentId) && !"0".equals(parentId)){
			Book pbook = bookService.get(parentId);
			book.setLevel((Integer.parseInt(pbook.getLevel()!=null&&!"".equals(pbook.getLevel())?pbook.getLevel():"0")+1)+"");
		}
		bookService.save(book);
		addMessage(redirectAttributes, "保存会计账本(科目)成功");
		return "redirect:"+Global.getAdminPath()+"/accountant/book/?repage";
	}
	
	@RequiresPermissions("accountant:book:edit")
	@RequestMapping(value = "delete")
	public String delete(Book book, RedirectAttributes redirectAttributes) {
		bookService.delete(book);
		addMessage(redirectAttributes, "删除会计账本(科目)成功");
		return "redirect:"+Global.getAdminPath()+"/accountant/book/?repage";
	}

	@RequiresPermissions("user")
	@ResponseBody
	@RequestMapping(value = "treeData")
	public List<Map<String, Object>> treeData(@RequestParam(required=false) String extId, HttpServletResponse response) {
		List<Map<String, Object>> mapList = Lists.newArrayList();
		List<Book> list = bookService.findList(new Book());
		for (int i=0; i<list.size(); i++){
			Book e = list.get(i);
			if (StringUtils.isBlank(extId) || (extId!=null && !extId.equals(e.getId()) && e.getParentIds().indexOf(","+extId+",")==-1)){
				Map<String, Object> map = Maps.newHashMap();
				map.put("id", e.getId());
				map.put("pId", e.getParentId());
				map.put("name", e.getCode()+" "+e.getName());
				mapList.add(map);
			}
		}
		return mapList;
	}
	
	@RequestMapping(value = "listView")
	public String listView(HttpServletRequest request, Model model) {
		List<Map<Object, Object>> mapList = Lists.newArrayList();
		Book form = new Book();
		Book b = new Book();
		b.setId("0");
		form.setParent(b);form.setCompanyId("");
		List<Book> nodes = bookService.findList(form);
		List<Map<String,Object>> tempDate=new ArrayList<Map<String,Object>>();
		for(Book node:nodes){
			Map<String,Object> obj=this.changeFunToMap(node);
			form.setParent(node);
			List<Book> bookList = bookService.findList(form);
			if(bookList!=null && bookList.size()>0){
				List<Map<String,Object>> chileList=new ArrayList<Map<String,Object>>();
				for (Book book : bookList) {
					Map<String,Object> childMap=this.changeFunToMap(book);
					chileList.add(childMap);
				}
				if(chileList.size()>0){
					obj.put("children",chileList);
				}
			}
			tempDate.add(obj);
		}
		List<Map<String,Object>> list=new ArrayList<Map<String,Object>>();
//		model.addAttribute("list", JSONUtils.toJSONString(tempDate));
		List<Book> books = bookService.findList(new Book());
		for (Book book : books) {
			Map<String,Object> obj=this.changeFunToMap2(book);
			list.add(obj);
		}
		model.addAttribute("list", JSONUtils.toJSONString(list));
		return "modules/accountant/bookListView";
	}
	public Map<String, Object> changeFunToMap(Book childNode) {
		Map<String,Object> obj=new HashMap<String, Object>();
		obj.put("name",childNode.getName());
		obj.put("id", childNode.getId());
		return obj;
	}
	public Map<String, Object> changeFunToMap2(Book childNode) {
		Map<String,Object> obj=new HashMap<String, Object>();
		obj.put("name",childNode.getName());
		obj.put("id", childNode.getId());
		if(childNode.getParent()!=null){
			obj.put("pId", childNode.getParent().getId());
			obj.put("open", true);
		}else{
			obj.put("pId", "0");
		}
		return obj;
	}
	
	@RequestMapping(value = "add")
	@ResponseBody
	public String add(HttpServletRequest request, Model model) {
		String ids = request.getParameter("ids");
		String[] idsArray = ids.split(",");
		for (String id : idsArray) {
			Book book = bookService.get(id);
			Book b = new Book();

		}
		return "success";
	}

	/**
	 * 树结构选择标签（treeselect.tag）
	 */
	@RequiresPermissions("user")
	@RequestMapping(value = "bookselect")
	public String bookselect(HttpServletRequest request, Model model) {

		String companyId = UserUtils.getUser().getCompany().getId();
		Book book = new Book();
//		book.setCompanyId(companyId);
		book.setLevel("2");
		book.setCategory("left");
		book.setAccountantCategory("assets");

		List<Book> assets_category = bookService.findByCategoryList(book);
		model.addAttribute("assets_category", assets_category);

		book.setCategory("left");
		book.setAccountantCategory("expenses");

		List<Book> expenses_category = bookService.findByCategoryList(book);
		model.addAttribute("expenses_category", expenses_category);

		book.setCategory("right");
		book.setAccountantCategory("owners_equity");

		List<Book> owners_equity_category = bookService.findByCategoryList(book);
		model.addAttribute("owners_equity_category", owners_equity_category);

		book.setCategory("right");
		book.setAccountantCategory("liabilities");

		List<Book> liabilities_category = bookService.findByCategoryList(book);
		model.addAttribute("liabilities_category", liabilities_category);

		book.setCategory("right");
		book.setAccountantCategory("income");

		List<Book> income_category = bookService.findByCategoryList(book);
		model.addAttribute("income_category", income_category);

		return "modules/accountant/tagbookselect";
	}
}