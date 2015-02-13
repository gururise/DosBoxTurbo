/*
 *  Copyright (C) 2002-2013  The DOSBox Team
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 */


#include "dosbox.h"
#include "inout.h"
#include "mem.h"
#include "setup.h"
#include "debug.h"
#include "callback.h"
#include "regs.h"
#include "voodoo_main.h"

#if defined(PCI_FUNCTIONALITY_ENABLED)

static Bit32u pci_caddress=0;

static Bitu pci_devices_installed=4;
static Bit8u pci_cfg_data[16][256];

// PCI address
// 31    - set for a PCI access
// 30-24 - 0
// 23-16 - bus number                             (0x00ff0000)
// 15-11 - device (slot)                          (0x0000f800)
// 10-8  - subfunction (a card can have multiple) (0x00000700)
// 7-0   - config register #                      (0x000000ff)

static void write_pci_addr(Bitu port,Bitu val,Bitu iolen) {

	LOG(LOG_PCI,LOG_WARN)("writing address %x",val);
	pci_caddress=val;
}

static Bitu read_pci_addr(Bitu port,Bitu iolen) {
	LOG(LOG_PCI,LOG_WARN)("reading address %x",pci_caddress);
	return pci_caddress;
}

extern void Voodoo_PCI_Enable(bool enable);

static void write_pci(Bitu port,Bitu val,Bitu iolen) {
	LOG(LOG_PCI,LOG_WARN)("write %x",val);
//	LOG_MSG("PCI WR len %d BUS%x DEV%02x FNC%02x REG%02x VAL%x", iolen,
//		(pci_caddress&0x00ff0000)>>16, (pci_caddress&0x0000f800)>>11,
//		(pci_caddress&0x00000700)>>8, (pci_caddress&0xff)+(port&3), val);

//	DEBUG_EnableDebugger();

	Bit8u devfunc, regnum;

	if ((pci_caddress & 0x80ff0000) == 0x80000000) {
		devfunc = (Bit8u)((pci_caddress >> 8) & 0xff);
		regnum = (Bit8u)((pci_caddress & 0xfc) + (port & 0x03));
		LOG(LOG_PCI,LOG_WARN)("reg %x fct %x (:=%x)",regnum,devfunc,val);
		if ((Bitu)(devfunc>>3)>=pci_devices_installed) return;
//		DEBUG_EnableDebugger();
		if (((regnum>=4) && (regnum<=7)) || (regnum==12) || (regnum==13) || (regnum>14)) {
			switch (devfunc>>3) {
				case 0:
					if ((regnum>=0x10) && (regnum<0x34)) return;
					LOG(LOG_PCI,LOG_WARN)("write PCI host %x",regnum);
					switch (iolen) {
						case 1:
							if ((regnum!=0x06) && (regnum!=0x0c)) pci_cfg_data[devfunc>>3][regnum++]=(Bit8u)(val&0xff);
							break;
						case 2:
							if ((regnum!=0x06) && (regnum!=0x0c)) pci_cfg_data[devfunc>>3][regnum++]=(Bit8u)(val&0xff);
							if ((regnum!=0x06) && (regnum!=0x0c)) pci_cfg_data[devfunc>>3][regnum]=(Bit8u)((val>>8)&0xff);
							break;
						case 4:
							if ((regnum!=0x06) && (regnum!=0x0c)) pci_cfg_data[devfunc>>3][regnum++]=(Bit8u)(val&0xff);
							if ((regnum!=0x06) && (regnum!=0x0c)) pci_cfg_data[devfunc>>3][regnum++]=(Bit8u)((val>>8)&0xff);
							if ((regnum!=0x06) && (regnum!=0x0c)) pci_cfg_data[devfunc>>3][regnum++]=(Bit8u)((val>>16)&0xff);
							if ((regnum!=0x06) && (regnum!=0x0c)) pci_cfg_data[devfunc>>3][regnum]=(Bit8u)((val>>24)&0xff);
							break;
						default:
							E_Exit("bad...");
					}
				case 1:
					if ((regnum>=0x30) && (regnum<0x34)) return;
					if ((regnum==0x04) || (regnum==0x06)) return;
					LOG(LOG_PCI,LOG_WARN)("write VGA device %x",regnum);
					switch (iolen) {
						case 1:
							pci_cfg_data[devfunc>>3][regnum++]=(Bit8u)(val&0xff);
							break;
						case 2:
							pci_cfg_data[devfunc>>3][regnum++]=(Bit8u)(val&0xff);
							pci_cfg_data[devfunc>>3][regnum]=(Bit8u)((val>>8)&0xff);
							break;
						case 4:
							pci_cfg_data[devfunc>>3][regnum++]=(Bit8u)(val&0xff);
							pci_cfg_data[devfunc>>3][regnum++]=(Bit8u)((val>>8)&0xff);
							pci_cfg_data[devfunc>>3][regnum++]=(Bit8u)((val>>16)&0xff);
							pci_cfg_data[devfunc>>3][regnum]=(Bit8u)((val>>24)&0xff);
							break;
						default:
							E_Exit("bad...");
					}
				case 2:
					if ((regnum==0x05) || (regnum==0x06)) return;
					if ((regnum==0x22) || (regnum==0x23)) return;
					if (regnum==0x04) val&=0x05;
					LOG(LOG_PCI,LOG_WARN)("write IDE device %x",regnum);
					switch (iolen) {
						case 1:
							pci_cfg_data[devfunc>>3][regnum++]=(Bit8u)(val&0xff);
							break;
						case 2:
							pci_cfg_data[devfunc>>3][regnum++]=(Bit8u)(val&0xff);
							if ((regnum==0x05) || (regnum==0x06)) return;
							if ((regnum==0x22) || (regnum==0x23)) return;
							if (regnum==0x04) val&=0x05;
							pci_cfg_data[devfunc>>3][regnum]=(Bit8u)((val>>8)&0xff);
							break;
						case 4:
							pci_cfg_data[devfunc>>3][regnum++]=(Bit8u)(val&0xff);
							if ((regnum==0x05) || (regnum==0x06)) return;
							if ((regnum==0x22) || (regnum==0x23)) return;
							if (regnum==0x04) val&=0x05;
							pci_cfg_data[devfunc>>3][regnum++]=(Bit8u)((val>>8)&0xff);
							if ((regnum==0x05) || (regnum==0x06)) return;
							if ((regnum==0x22) || (regnum==0x23)) return;
							if (regnum==0x04) val&=0x05;
							pci_cfg_data[devfunc>>3][regnum++]=(Bit8u)((val>>16)&0xff);
							if ((regnum==0x05) || (regnum==0x06)) return;
							if ((regnum==0x22) || (regnum==0x23)) return;
							if (regnum==0x04) val&=0x05;
							pci_cfg_data[devfunc>>3][regnum]=(Bit8u)((val>>24)&0xff);
							break;
						default:
							E_Exit("bad...");
					}
				case 3: // Voodoo
//					LOG_MSG("SST1 WR len %d REG %02x VAL%x", iolen,
//						(pci_caddress&0xff)+(port&3), val);
					if (regnum==0x40)
						pci_cfg_data[devfunc>>3][regnum] = val;
					else if (regnum==0x13)
						pci_cfg_data[3][0x13] = val;
					else if (regnum==0xc0)
						Voodoo_PCI_Enable(true);
					else if (regnum==0xe0)
						Voodoo_PCI_Enable(false);
					break;
				default:
					E_Exit("bad...");
			}
		}
	}
}
static Bitu read_pci2(Bitu port,Bitu iolen);
static Bitu read_pci(Bitu port,Bitu iolen) {
	Bitu retval = read_pci2(port,iolen);
//	LOG_MSG("PCI RD len %d BUS%x DEV%02x FNC%02x REG%02x VAL%x", iolen,
//		(pci_caddress&0x00ff0000)>>16, (pci_caddress&0x0000f800)>>11,
//		(pci_caddress&0x00000700)>>8, (pci_caddress&0xff)+(port&3), retval);
	return retval;
}

static Bitu read_pci2(Bitu port,Bitu iolen) {
	LOG(LOG_PCI,LOG_WARN)("read pci (%x)",pci_caddress);

//	DEBUG_EnableDebugger();

	Bit8u devfunc, regnum;

	if ((pci_caddress & 0x80ff0000) == 0x80000000) {
		devfunc = (Bit8u)((pci_caddress >> 8) & 0xff);
		regnum = (Bit8u)((pci_caddress & 0xfc) + (port & 0x03));
		if ((Bitu)(devfunc>>3)>=pci_devices_installed) return 0xffffffff;
		LOG(LOG_PCI,LOG_WARN)("reg %x fct %x; addr %x",regnum,devfunc,pci_caddress);
		switch (iolen) {
			case 1: return pci_cfg_data[devfunc>>3][regnum];
			case 2: return pci_cfg_data[devfunc>>3][regnum] | (pci_cfg_data[devfunc>>3][regnum+1]<<8);
			case 4: return  pci_cfg_data[devfunc>>3][regnum] | (pci_cfg_data[devfunc>>3][regnum+1]<<8) |
							(pci_cfg_data[devfunc>>3][regnum+2]<<16) | (pci_cfg_data[devfunc>>3][regnum+3]<<24);
			default:
				E_Exit("bad...");
		}
		return 0xffffffff;
	} else {
		return 0xffffffff;
	}
}


static Bitu PCI_PM_Handler() {
	LOG(LOG_PCI,LOG_WARN)("PCI PMODE handler, function %x",reg_ax);
//	DEBUG_EnableDebugger();
	return CBRET_NONE;
}

Bitu pci_callback=0;



class PCI:public Module_base{
public:
	PCI(Section* configuration):Module_base(configuration){
		IO_RegisterWriteHandler(0xcf8,write_pci_addr,IO_MD);
		IO_RegisterReadHandler(0xcf8,read_pci_addr,IO_MD);

		IO_RegisterWriteHandler(0xcfc,write_pci,IO_MB);
		IO_RegisterReadHandler(0xcfc,read_pci,IO_MB);
		IO_RegisterWriteHandler(0xcfd,write_pci,IO_MB);
		IO_RegisterReadHandler(0xcfd,read_pci,IO_MB);
		IO_RegisterWriteHandler(0xcfe,write_pci,IO_MB);
		IO_RegisterReadHandler(0xcfe,read_pci,IO_MB);
		IO_RegisterWriteHandler(0xcff,write_pci,IO_MB);
		IO_RegisterReadHandler(0xcff,read_pci,IO_MB);

		for (Bitu dev=0; dev<16; dev++)
			for (Bitu i=0; i<256; i++) pci_cfg_data[dev][i] = 0;

		// init
		pci_cfg_data[0][0x00] = 0x86;
		pci_cfg_data[0][0x01] = 0x80;
		pci_cfg_data[0][0x02] = 0x37;
		pci_cfg_data[0][0x03] = 0x12;
		pci_cfg_data[0][0x0b] = 0x06;

		// reset
		pci_cfg_data[0][0x04] = 0x06;
		pci_cfg_data[0][0x05] = 0x00;
		pci_cfg_data[0][0x06] = 0x80;
		pci_cfg_data[0][0x07] = 0x02;
		pci_cfg_data[0][0x0d] = 0x00;
		pci_cfg_data[0][0x0f] = 0x00;
		pci_cfg_data[0][0x50] = 0x00;
		pci_cfg_data[0][0x51] = 0x01;
		pci_cfg_data[0][0x52] = 0x00;
		pci_cfg_data[0][0x53] = 0x80;
		pci_cfg_data[0][0x54] = 0x00;
		pci_cfg_data[0][0x55] = 0x00;
		pci_cfg_data[0][0x56] = 0x00;
		pci_cfg_data[0][0x57] = 0x01;
		pci_cfg_data[0][0x58] = 0x10;


		// S3 Video
		pci_cfg_data[1][0x00] = 0x33;
		pci_cfg_data[1][0x01] = 0x53;
		pci_cfg_data[1][0x02] = 0x11;
		pci_cfg_data[1][0x03] = 0x88;
		// class code
		pci_cfg_data[1][0x09] = 0x00;
		pci_cfg_data[1][0x0a] = 0x00;
		pci_cfg_data[1][0x0b] = 0x03;
		// header type
		pci_cfg_data[1][0x0e] = 0x00;

		// reset
		pci_cfg_data[1][0x04] = 0x03;
		pci_cfg_data[1][0x05] = 0x00;
		pci_cfg_data[1][0x06] = 0x00;
		pci_cfg_data[1][0x07] = 0x02;


		// init
	//	pci_cfg_data[2][0x00] = 0x86;
	//	pci_cfg_data[2][0x01] = 0x80;
		pci_cfg_data[2][0x00] = 0xff;
		pci_cfg_data[2][0x01] = 0xff;

		pci_cfg_data[2][0x02] = 0x10;
		pci_cfg_data[2][0x03] = 0x70;
		pci_cfg_data[2][0x09] = 0x80;
		pci_cfg_data[2][0x0a] = 0x01;
		pci_cfg_data[2][0x0b] = 0x01;
		pci_cfg_data[2][0x0e] = 0x00;

		// reset
		pci_cfg_data[2][0x04] = 0x01;
		pci_cfg_data[2][0x05] = 0x00;
		pci_cfg_data[2][0x06] = 0x80;
		pci_cfg_data[2][0x07] = 0x02;

		// ata0 present
		pci_cfg_data[2][0x40] = 0x80;
		pci_cfg_data[2][0x41] = 0x00;
		// ata1 disabled
		pci_cfg_data[2][0x42] = 0x80;
		pci_cfg_data[2][0x43] = 0x00;

		pci_cfg_data[2][0x44] = 0x00;

/* 3dfx voodoo */
		// ID
		pci_cfg_data[3][0x00] = 0x1A;
		pci_cfg_data[3][0x01] = 0x12;
		pci_cfg_data[3][0x02] = 0x01;
		pci_cfg_data[3][0x03] = 0x00;
		// class code
		pci_cfg_data[3][0x0b] = 0x00;
		pci_cfg_data[3][0x0a] = 0x00;
		pci_cfg_data[3][0x09] = 0x00;
		// header type
		pci_cfg_data[3][0x0e] = 0x00;
		// command reg
		pci_cfg_data[3][0x04] = 0x02;
		// revision
		pci_cfg_data[3][0x08] = 0x01;
		// memBaseAddr: size is 16MB,
		pci_cfg_data[3][0x10] = 0;
		pci_cfg_data[3][0x11] = 0;
		pci_cfg_data[3][0x12] = 0;
		pci_cfg_data[3][0x13] = (VOODOO_MEM >> 24) & 0xFF;

/////////////////////////////////////////////////////////////////////////////////

		pci_callback=CALLBACK_Allocate();
		CALLBACK_Setup(pci_callback,&PCI_PM_Handler,CB_IRETD,"PCI PM");

	//	if (memw[$f000:i]=$5024) and (memw[$f000:i+2]=$5249) then failed:=false;
		PhysPt pci_routing=0xf9000;
		phys_writed(pci_routing+0x00,0x52495024);		// signature
		phys_writew(pci_routing+0x04,0x0100);			// version
		phys_writew(pci_routing+0x06,32 + (6 * 16));	// table size
		phys_writeb(pci_routing+0x08,0x00);				// router bus
		phys_writeb(pci_routing+0x09,0x08);				// router dev func
		phys_writew(pci_routing+0x0a,0x0000);			// exclusive IRQs
		phys_writew(pci_routing+0x0c,0x8086);			// vendor ID
		phys_writew(pci_routing+0x0e,0x7000);			// device ID
		phys_writew(pci_routing+0x10,0x0000);			// miniport data
		phys_writew(pci_routing+0x12,0x0000);			// miniport data
		phys_writeb(pci_routing+0x1f,0x07);				// checksum

		pci_routing+=0x20;
		for (Bitu i=0; i<6; i++) {
			phys_writeb(pci_routing+0x00,0x00);				// bus number
			phys_writeb(pci_routing+0x01,0x08+i*8);			// device number
			pci_routing+=0x02;
			for (Bitu link=0; link<4; link++) {
				phys_writeb(pci_routing+0x00,0x60+((i+link)&3));	// link value
				phys_writew(pci_routing+0x01,0xdef8);				// bitmap
				pci_routing+=0x03;
			}
			phys_writeb(pci_routing+0x00,i);				// slot
			phys_writeb(pci_routing+0x01,0x00);
			pci_routing+=0x02;
		}

		phys_writeb(pci_routing+0x00,11);		// irq
		phys_writeb(pci_routing+0x01,10);		// irq
		phys_writeb(pci_routing+0x02,9);		// irq
		phys_writeb(pci_routing+0x03,5);		// irq

	}

	~PCI(){

	}
};

static PCI* pci_test;

void PCI_ShutDown(Section* sec){
	delete pci_test;
}

void PCI_Init(Section* sec) {
	pci_test = new PCI(sec);
	sec->AddDestroyFunction(&PCI_ShutDown,false);
}

#endif
